import testcommon
from dstack.agent.event import EventClient


def run_agent_connect():
    client = EventClient("http://localhost:8080/v1", workers=1)
    client.run(["*"])


if __name__ == "__main__":
    run_agent_connect()