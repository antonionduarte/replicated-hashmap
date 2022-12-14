import subprocess
import os

from config import SingleExperiment

BASE_HASHAPP_PORT = 35000
BASE_STATEMACHINE_PORT = 3000
EXPERIMENTS_FOLDER = "analysis/experiments/"


# TODO for you sweet Diogo:
# 1) YCSB working 
# 2) Waiting before processes finish running before cleaning docker and moving to next experiment
# 3) Capture the logs from stdout YCSB. Additional issue: docker doesn't to work very well with log4j to stdout.

# this function spits out the logs in JSON to the output/{number_clients}_clients folder

# could as well be output/{number_clients}.json
def process_log(filename: str, logs: list[str]):
    f = open(filename, "w")
    for line in logs:
        f.write(line + "\n")


def output_filename(config: SingleExperiment):
    os.makedirs(f"./{EXPERIMENTS_FOLDER}exp_set{config.num_exp_set}/", exist_ok=True)
    return f"./{EXPERIMENTS_FOLDER}exp_set{config.num_exp_set}/" \
           f"{config.number_clients}C_" \
           f"{config.number_replicas}R_" \
           f"{config.payload}B_" \
           f"{config.percentage_reads}_{config.percentage_writes}" \
           f".log"


def num_exp_set():
    num = 1
    while True:
        if f"exp_set{num}" not in os.listdir(EXPERIMENTS_FOLDER):
            break
        num += 1
    return num


def clean_docker_environment():
    cmd = "docker kill $(docker ps -aq); docker rm $(docker ps -aq)"
    subprocess.run(cmd, shell=True)


def spawn_ycsb_clients_docker(config: SingleExperiment):
    membership_str = generate_membership_str(config.number_replicas, BASE_HASHAPP_PORT)

    cwd = os.getcwd()
    args = [
        "docker",
        "run",
        f"--name=client_process",
        "-it",
        "--network=host",

        # Volumes:
        "-v", f"{cwd}/client/asd-client.jar:/usr/local/client/asd-client.jar",
        "-v", f"{cwd}/client/config.properties:/usr/local/client/config.properties",
        "-v", f"{cwd}/client/exec.sh:/usr/local/client/exec.sh",
        "-v", f"{cwd}/client/log4j2.xml:/usr/local/client/log4j2.xml",

        "--workdir=/usr/local/client/",

        "docker.io/amazoncorretto:19",
        "/bin/sh", "exec.sh",
        f"{config.number_clients}",
        f"{config.payload}",
        f"{membership_str}",
        f"{config.percentage_reads}",
        f"{config.percentage_writes}",
    ]
    results = subprocess.run(args, capture_output=True, text=True).stdout.splitlines()
    process_log(output_filename(config), list[str](results))


def spawn_replica_java_docker(port: int, config: SingleExperiment):
    real_port_sm = BASE_STATEMACHINE_PORT + port
    real_port_ha = BASE_HASHAPP_PORT + port
    membership_str = generate_membership_str(config.number_replicas, BASE_STATEMACHINE_PORT)

    cwd = os.getcwd()
    args = [
        "docker",
        "run",
        f"--name=paxos_{port}",
        "-itd",
        "--network=host",

        # Volumes:
        "-v",
        f"{cwd}/target/asdProj.jar:/usr/local/app.jar",

        "-v",
        f"{cwd}/config.properties:/usr/local/config.properties",

        "-v",
        f"{cwd}/log4j2.xml:/usr/local/log4j2.xml",

        "--workdir=/usr/local/",
        "docker.io/amazoncorretto:19",

        "java",
        "-cp",
        "/usr/local/app.jar",
        "asd.Main",
        "babel_address=localhost",
        f"statemachine_port={real_port_sm}",
        f"hashapp_port={real_port_ha}",
        f"statemachine_initial_membership={membership_str}"
    ]

    subprocess.run(args)
    # process.wait()
    # out, _ = process.communicate()
    # clean_docker_environment()


def generate_membership_str(num_replicas, base_port) -> str:
    str = f"localhost:{base_port}"
    for i in range(base_port + 1, base_port + num_replicas):
        str += f",localhost:{i}"
    return str
