import subprocess 
import time
import os 


BASE_HASHAPP_PORT = 35000
BASE_STATEMACHINE_PORT = 3000


def spawn_ycsb_clients_docker(num_replicas: int):
    return None


def spawn_paxos_java_docker(port: int, num_replicas: int):
    real_port_sm = BASE_STATEMACHINE_PORT + port
    real_port_ha = BASE_HASHAPP_PORT + port
    membership_str = generate_membership_str(num_replicas)

    cwd = os.getcwd()
    args = [
        "docker",
        "run",
        f"--name=paxos_{port}"
        "-itd",
        "--network=host",
        "-v"
        f"{cwd}/target/asdProj.jar:/usr/local/app.jar"
        # TODO: Need to mount other directories
        "--workdir=/usr/local/",
        "docker.io/amazoncorretto:19",

        "java",
        "-cp",
        "/usr/local/app.jar",
        "asd.PaxosMain",
        "babel_address=localhost",
        f"statemachine_port={real_port_sm}",
        f"hashapp_port={real_port_ha}"
        f"statemachine_initial_membership={membership_str}"
    ]


def generate_membership_str(num_replicas) -> str:
    str = f"localhost:{BASE_STATEMACHINE_PORT}"
    for i in range(BASE_STATEMACHINE_PORT + 1, BASE_STATEMACHINE_PORT + num_replicas):
        str += f",localhost:{i}"
    return str
