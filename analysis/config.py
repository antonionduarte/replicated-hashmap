import json
from dataclasses import dataclass

class Config:
    number_clients: int
    number_replicas: int
    percentage_writes: int 
    percentage_reads: int
    min_clients: int
    max_clients: int

    def __init__(self, config):
        self.min_clients = config['min-clients']
        self.max_clients = config['max-clients']
        self.step = config['step']
        self.number_replicas = config['number-replicas']
        self.percentage_writes = config['percentage-writes']
        self.percentage_reads = config['percentage-reads']


@dataclass
class SingleExperiment:
    number_clients: int 
    number_replicas: int 
    percentage_writes: int 
    percentage_reads: int


# Divide a configuration file into several executable experiments
def prepare_experiments(config: Config) -> list[SingleExperiment]:
    experiments = []
    number_clients = config.min_clients
    while number_clients <= config.max_clients:
        experiments.append(SingleExperiment( 
            config.min_clients,
            config.number_replicas, 
            config.percentage_writes, 
            config.percentage_reads))
        number_clients += config.step
    return experiments


def parse_config(config_path: str) -> Config:
    config_json = json.loads(open(config_path).read())
    config = Config(config_json)
    return config
