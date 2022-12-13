import subprocess 
import os 

from config import SingleExperiment


BASE_HASHAPP_PORT = 35000
BASE_STATEMACHINE_PORT = 3000

# TODO for you sweet Diogo:
# 1) YCSB working 
# 2) Waiting before processes finish running before cleaning docker and moving to next experiment
# 3) Capture the logs from stdout YCSB. Additional issue: docker doesn't to work very well with log4j to stdout.

# this function spits out the logs in JSON to the output/{number_clients}_clients folder
# could as well be output/{number_clients}.json
def process_log(logs: str):
    return None

# create the output folder if not exists
def prepare_environment():
    return None


def clean_docker_environment():
    cmd = "docker kill $(docker ps -aq); docker rm $(docker ps -aq)"
    subprocess.run(cmd, shell=True)


def spawn_ycsb_clients_docker(config: SingleExperiment):
    number_clients = config.number_clients
    membership_str = generate_membership_str(config.number_replicas, BASE_HASHAPP_PORT)

    cwd =  os.getcwd()
    args = [
        "docker",
        "run",
        f"--name=client_process",
        "-itd",
        "--network=host",

        # Volumes:
        "-v",
        f"{cwd}/output:/output", # output is the volume the output should go to

        "-v",
        f"{cwd}/client/asd-client.jar:/usr/local/asd-client.jar",

        "-v",
        f"{cwd}/client/config.properties:/usr/local/config.properties",

        "-v",
        f"{cwd}/client/log4j2.xml:/usr/local/log4j2.xml",

        "--workdir=/usr/local/",
        "docker.io/amazoncorretto:19",

        "java",
        "-Dlog4j.configurationFile=log4j2.xml",
        "-DlogFilename=clientLog.log",
        "-cp",
        "asd-client.jar",
        "site.ycsb.Client",
        "-t",
        "-s",
        "-P",
        "config.properties",
        f"-threads {config.number_clients}",
        "-p",
        "fieldlength=1024",
        f"-p hosts={membership_str}",
        f"-p readproportion={config.percentage_reads}",
        "-p", 
        "updateproportion={config.percentage_writes}"
    ]

    subprocess.run(args)
    # clean_docker_environment()


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
