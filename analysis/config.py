import json
from dataclasses import dataclass


class Config:
    number_clients: int
    number_replicas: int
    percentage_writes: int
    percentage_reads: int
    min_clients: int
    max_clients: int
    payload: int

    def __init__(self, config):
        self.min_clients = config['min-clients']
        self.max_clients = config['max-clients']
        self.client_step = config['client-step']
        self.min_replicas = config['min-replicas']
        self.max_replicas = config['max-replicas']
        self.replica_step = config['replica-step']
        self.percentage_writes = config['percentage-writes']
        self.percentage_reads = config['percentage-reads']
        self.payload = config['payload']


@dataclass
class SingleExperiment:
    exp_name: str
    number_clients: int
    number_replicas: int
    percentage_writes: int
    percentage_reads: int
    payload: int


# Divide a configuration file into several executable experiments
def prepare_experiments(config: Config, exp_name: str) -> list[SingleExperiment]:
    experiments = []
    number_clients = config.min_clients
    while number_clients <= config.max_clients:
        number_replicas = config.min_replicas
        while number_replicas <= config.max_replicas:
            experiments.append(SingleExperiment(
                exp_name,
                number_clients,
                number_replicas,
                config.percentage_writes,
                config.percentage_reads,
                config.payload))
            number_replicas += config.replica_step
        number_clients += config.client_step
    return experiments


def parse_config(config_path: str) -> Config:
    config_json = json.loads(open(config_path).read())
    config = Config(config_json)
    return config
