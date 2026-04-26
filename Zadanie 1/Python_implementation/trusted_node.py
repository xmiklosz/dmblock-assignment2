# AI: Implementacia vytvorena s pomocou Claude AI (Anthropic) - Python rewrite (bonus 5 bodov).
# TrustedNode - byzantsky konsenzus (Faza 2).


class Transaction2:
    """Jednoducha transakcia pre fazu 2 (len ID)."""

    def __init__(self, tx_id):
        self.id = tx_id

    def __eq__(self, other):
        if not isinstance(other, Transaction2):
            return False
        return self.id == other.id

    def __hash__(self):
        return hash(self.id)


class TrustedNode:
    """Implementacia dovernhodneho uzla pre byzantsky konsenzus.
    Pristup: akceptuj vsetky transakcie od followees (jednoduchy, efektivny)."""

    def __init__(self, num_nodes, malicious, followees):
        self.num_nodes = num_nodes
        self.malicious = malicious
        self.followees = followees
        self.consensus_txs = set()

    def initial_proposal(self):
        return set(self.consensus_txs)

    def followees_receive(self, candidates):
        for tx_id, sender in candidates:
            if not self.followees[sender]:
                continue
            self.consensus_txs.add(Transaction2(tx_id))

    def get_consensus(self):
        return set(self.consensus_txs)
