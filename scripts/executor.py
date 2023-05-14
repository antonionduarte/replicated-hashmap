#!/usr/bin/python3

import os
import time
import json
import base64
import dataclasses
import subprocess
import argparse
import datetime

from typing import Generator, Union

EXPERIMENTS_FOLDER = "analysis/experiments/"
STATEMACHINE_PORT = 3000
HASHAPP_PORT = 4000


@dataclasses.dataclass(frozen=True, eq=True)
class Machine:
    cluster: str
    id: int

    def __str__(self):
        return f"{self.cluster}-{self.id}"

    def __lt__(self, other):
        return (self.cluster, self.id) < (other.cluster, other.id)


@dataclasses.dataclass(frozen=True, eq=True)
class ExperimentConfig:
    # Number of clients
    number_clients: int
    # Number of replicas, one per machine
    number_replicas: int
    # Number of operations to perform
    number_operations: int
    # Percentage of operations that are writes, between 0 and 100
    # Read percentage is 100 - write percentage
    percentage_writes: int
    # Paxos variant to use. One of 'single' or 'multi'
    paxos_variant: str
    # Payload size in bytes
    payload: int

    def to_json(self):
        return json.dumps(dataclasses.asdict(self))

    @staticmethod
    def from_json(json_str: str):
        return ExperimentConfig(**json.loads(json_str))

    def to_base64(self):
        return base64.b64encode(self.to_json().encode("utf-8")).decode("utf-8")

    @staticmethod
    def from_base64(base64_str: str):
        return ExperimentConfig.from_json(
            base64.b64decode(base64_str.encode("utf-8")).decode("utf-8")
        )


@dataclasses.dataclass(frozen=True, eq=True)
class ExperimentOuput:
    config: ExperimentConfig
    output: str

    def to_json(self):
        return json.dumps(dataclasses.asdict(self), indent=4)

    @staticmethod
    def from_json(json_str: str):
        return ExperimentOuput(**json.loads(json_str))


def hostname_to_machine(hostname: str) -> Machine:
    cluster, id = hostname.split("-")
    return Machine(cluster, int(id))


def machine_to_hostname(machine: Machine) -> str:
    return f"{machine.cluster}-{machine.id}"


def local_hostname() -> str:
    return subprocess.check_output(["hostname"]).decode("utf-8").strip()


def local_machine() -> Machine:
    return hostname_to_machine(local_hostname())


def job_id() -> int:
    return int(os.environ["OAR_JOB_ID"])


def job_hostnames() -> list[str]:
    return list(
        sorted(
            subprocess.check_output(["oarprint", "host"])
            .decode("utf-8")
            .strip()
            .split("\n")
        )
    )


def job_machines() -> list[Machine]:
    return list(sorted([hostname_to_machine(hostname) for hostname in job_hostnames()]))


def machine_clear_docker(machines: Union[Machine, list[Machine]]):
    if isinstance(machines, Machine):
        machines = [machines]
    procs = []
    for machine in machines:
        hostname = machine_to_hostname(machine)
        cmd = f"oarsh {hostname} 'docker kill $(docker ps -aq); docker rm $(docker ps -aq)'"
        print(f"Clearing docker on {hostname}")
        proc = subprocess.Popen(
            cmd, shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
        )
        procs.append(proc)
    for proc in procs:
        proc.wait()


def machine_ensure_java(machines: Union[Machine, list[Machine]]):
    if isinstance(machines, Machine):
        machines = [machines]
    procs = []
    for machine in machines:
        hostname = machine_to_hostname(machine)
        cmd = f"oarsh {hostname} 'docker pull amazoncorretto:17'"
        print(f"Pulling java docker image on {hostname}")
        proc = subprocess.Popen(
            cmd, shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
        )
        procs.append(proc)
    for proc in procs:
        proc.wait()


def cmd_run(args):
    cfg = ExperimentConfig(
        number_clients=args.clients,
        number_replicas=args.replicas,
        number_operations=args.operations,
        percentage_writes=args.writes,
        paxos_variant=args.paxos_variant,
        payload=args.payload,
    )
    num_machines = cfg.number_replicas + 1
    machines = job_machines()
    if len(machines) < num_machines:
        raise Exception(
            f"Number of machines ({len(machines)}) does not match number of replicas ({num_machines})"
        )

    machines = machines[:num_machines]
    replica_machines = machines[: cfg.number_replicas]
    client_machine = machines[cfg.number_replicas]
    statemachine_membership = ",".join(
        [
            f"{machine_to_hostname(machine)}:{STATEMACHINE_PORT}"
            for machine in replica_machines
        ]
    )
    hashapp_membership = ",".join(
        [
            f"{machine_to_hostname(machine)}:{HASHAPP_PORT}"
            for machine in replica_machines
        ]
    )

    machine_clear_docker(machines)
    machine_ensure_java(machines)

    print("Spawing replicas")
    cwd = os.getcwd()
    replica_procs = []
    for machine in replica_machines:
        replica_procs.append(
            subprocess.Popen(
                [
                    "oarsh",
                    machine_to_hostname(machine),
                    f"python3 {cwd}/scripts/executor.py --working-dir={cwd} replica --config={cfg.to_base64()} --statemachine-membership={statemachine_membership}",
                ]
            )
        )

    time.sleep(10)
    print("Spawning clients")
    client_proc = subprocess.run(
        [
            "oarsh",
            machine_to_hostname(client_machine),
            f"python3 {cwd}/scripts/executor.py --working-dir={cwd} client --config={cfg.to_base64()} --hashapp-membership={hashapp_membership}",
        ],
        capture_output=True,
        text=True,
    )
    output_path = args.output
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    output = ExperimentOuput(config=cfg, output=client_proc.stdout)
    print(f"Writing output to {output_path}")
    with open(output_path, "w") as f:
        f.write(output.to_json())

    machine_clear_docker(replica_machines)


def cmd_replica(args):
    cwd = args.working_dir
    cfg = ExperimentConfig.from_base64(args.config)
    os.chdir(cwd)

    args = [
        "docker",
        "run",
        "--rm",
        "--network=host",
        # Volumes:
        "-v",
        f"{cwd}/target/asdProj.jar:/usr/local/app.jar",
        "-v",
        f"{cwd}/config.properties:/usr/local/config.properties",
        "-v",
        f"{cwd}/log4j2.xml:/usr/local/log4j2.xml",
        "--workdir=/usr/local/",
        "docker.io/amazoncorretto:17",
        "java",
        "-cp",
        "/usr/local/app.jar",
        "asd.Main",
        f"babel_address={local_hostname()}",
        f"statemachine_port={STATEMACHINE_PORT}",
        f"hashapp_port={HASHAPP_PORT}",
        f"statemachine_initial_membership={args.statemachine_membership}",
        f"paxos_variant={cfg.paxos_variant}",
    ]

    subprocess.run(args)


def cmd_client(args):
    cwd = args.working_dir
    cfg = ExperimentConfig.from_base64(args.config)
    os.chdir(cwd)

    args = [
        "docker",
        "run",
        "--rm",
        "--network=host",
        # Volumes:
        "-v",
        f"{cwd}/client/asd-client.jar:/usr/local/client/asd-client.jar",
        "-v",
        f"{cwd}/client/config.properties:/usr/local/client/config.properties",
        "-v",
        f"{cwd}/client/exec.sh:/usr/local/client/exec.sh",
        "-v",
        f"{cwd}/client/log4j2.xml:/usr/local/client/log4j2.xml",
        "--workdir=/usr/local/client/",
        "docker.io/amazoncorretto:17",
        "/bin/sh",
        "exec.sh",
        f"{cfg.number_clients}",
        f"{cfg.payload}",
        f"{args.hashapp_membership}",
        f"{cfg.percentage_writes}",
        f"{100-cfg.percentage_writes}",
        "-p",
        f"operationcount={cfg.number_operations}",
    ]
    subprocess.run(args)


def cmd_run_generator(args):
    def output_filename_from_config(cfg: ExperimentConfig) -> str:
        return (
            f"{cfg.number_clients}C_"
            f"{cfg.number_replicas}R_"
            f"{cfg.number_operations}O_"
            f"{cfg.percentage_writes}W_"
            f"{cfg.paxos_variant}_"
            f"{cfg.payload}B"
            f".json"
        )

    def config_generator(args) -> Generator[ExperimentConfig, None, None]:
        for payload in args.payload:
            for variant in args.variant:
                for write in args.writes:
                    for clients in range(
                        args.clients_from, args.clients_to, args.clients_step
                    ):
                        yield ExperimentConfig(
                            number_clients=clients,
                            number_replicas=3,
                            number_operations=args.operations,
                            percentage_writes=write,
                            paxos_variant=variant,
                            payload=payload,
                        )

    def read_existing_configs(args) -> Generator[ExperimentConfig, None, None]:
        for filename in os.listdir(args.output_dir):
            with open(os.path.join(args.output_dir, filename), "r") as f:
                yield ExperimentOuput.from_json(f.read()).config

    os.makedirs(args.output_dir, exist_ok=True)
    existing = list(read_existing_configs(args))
    required = list(config_generator(args))
    missing = [cfg for cfg in required if cfg not in existing]

    for cfg in missing:
        print(f"Running experiment with config: {cfg}")
        if args.dry_run:
            continue
        subprocess.run(
            [
                "python3",
                "scripts/executor.py",
                "run",
                "--clients",
                str(cfg.number_clients),
                "--replicas",
                str(cfg.number_replicas),
                "--writes",
                str(cfg.percentage_writes),
                "--paxos-variant",
                cfg.paxos_variant,
                "--operations",
                str(cfg.number_operations),
                "--output",
                os.path.join(args.output_dir, output_filename_from_config(cfg)),
            ]
        ).check_returncode()


def main():
    parser = argparse.ArgumentParser(description="Run experiments")
    parser.add_argument(
        "--working-dir", type=str, help="Working directory", default=f"{os.getcwd()}"
    )

    subparsers = parser.add_subparsers(help="sub-command help", dest="command")

    run_parser = subparsers.add_parser("run", help="Run experiments")
    run_parser.add_argument(
        "--clients", type=int, help="Number of clients", default=1000
    )
    run_parser.add_argument(
        "--replicas", type=int, help="Number of replicas", default=3
    )
    run_parser.add_argument(
        "--writes", type=int, help="Percentage of writes", default=50
    )
    run_parser.add_argument(
        "--paxos-variant", type=str, help="Paxos variant", default="multi"
    )
    run_parser.add_argument(
        "--operations",
        type=int,
        help="Number of operations to run",
        default=100000,
    )
    run_parser.add_argument(
        "--output",
        type=str,
        help="Output file",
        default=f"experiment_{int(datetime.datetime.utcnow().timestamp())}.json",
    )
    run_parser.add_argument("--payload", type=int, help="Payload size", default=1024)
    run_parser.add_argument("--exp-name", type=str, help="Experiment name")
    run_parser.set_defaults(func=cmd_run)

    replica_parser = subparsers.add_parser("replica", help="Run replica")
    replica_parser.add_argument("--config", type=str, help="Experiment config")
    replica_parser.add_argument(
        "--statemachine-membership", type=str, help="Membership"
    )
    replica_parser.set_defaults(func=cmd_replica)

    client_parser = subparsers.add_parser("client", help="Run client")
    client_parser.add_argument("--config", type=str, help="Experiment config")
    client_parser.add_argument("--hashapp-membership", type=str, help="Membership")
    client_parser.set_defaults(func=cmd_client)

    run_generator_parser = subparsers.add_parser("run-generator", help="Run generator")
    run_generator_parser.add_argument("--dry-run", action="store_true")
    run_generator_parser.add_argument(
        "--output-dir",
        type=str,
        help="Output directory",
        default=f"analysis/experiments",
    )
    run_generator_parser.add_argument(
        "--variant",
        type=str,
        action="append",
        help="Paxos variants",
        default=["single", "multi"],
    )
    run_generator_parser.add_argument(
        "--operations", type=int, help="Number of operations to run", default=50000
    )
    run_generator_parser.add_argument(
        "--clients-from",
        type=int,
        help="Number of clients to start from",
        default=100,
    )
    run_generator_parser.add_argument(
        "--clients-to", type=int, help="Number of clients to end at", default=2500
    )
    run_generator_parser.add_argument(
        "--clients-step", type=int, help="Number of clients to step by", default=100
    )
    run_generator_parser.add_argument(
        "--writes", type=int, action="append", help="Percentage of writes", default=[50]
    )
    run_generator_parser.add_argument(
        "--payload", type=int, action="append", help="Payload size", default=[1024]
    )
    run_generator_parser.set_defaults(func=cmd_run_generator)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
