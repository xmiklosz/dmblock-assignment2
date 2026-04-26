# CommitMarket — Commit-Reveal Prediction Market dApp

> **Course:** Digital Currencies and Blockchain (DMBLOCK) — Assignment 2  
> **Team:** xmiklosz  
> **Deadline:** 10 May 2026

---

## Project Description

CommitMarket is a decentralized prediction market where users can create binary YES/NO questions, stake ETH on outcomes, and resolve them through a **commit-reveal voting scheme**.

The core novelty over a standard prediction market is the commit-reveal resolution mechanism: resolvers first submit a hash of their answer (commit phase), then reveal the actual answer after a deadline. This prevents front-running and last-minute bandwagon voting — no resolver can see how others voted before submitting their own answer.

Additional original design decisions:
- **Multi-resolver quorum** — resolution requires a minimum number of resolvers (e.g. 3). Minority resolvers who voted against the quorum are slashed; majority resolvers earn a share of the loser pot.
- **Creator bond** — market creators lock a small ETH bond. If the market expires without enough resolvers participating (no quorum), the bond is distributed to stakers as compensation and the market is marked INVALID.
- **No oracle dependency** — resolution is fully decentralized through human resolvers with economic incentives, no Chainlink or external data feed required.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                     Frontend                        │
│         React + TypeScript + Vite + Tailwind        │
│         ethers.js v6 + MetaMask                     │
│         Deployed: dmblock-assignment2.vercel.app    │
└──────────────────────┬──────────────────────────────┘
                       │ ethers.js RPC calls
┌──────────────────────▼──────────────────────────────┐
│              PredictionMarket.sol                   │
│         Deployed on Ethereum Sepolia                │
│                                                     │
│  Lifecycle: STAKING → COMMITTING → REVEALING        │
│             → RESOLVED (YES/NO) or INVALID          │
│                                                     │
│  Key mechanisms:                                    │
│  - ReentrancyGuard (OpenZeppelin)                   │
│  - Checks-Effects-Interactions pattern              │
│  - Commit hash: keccak256(outcome ++ salt ++ addr)  │
│  - Slash pool redistributed to quorum winners       │
└─────────────────────────────────────────────────────┘
```

### Smart Contract Flow

1. **Create** — creator submits question, resolution time, commit/reveal windows, quorum size, and ETH bond
2. **Stake** — any user stakes ETH on YES or NO before resolution time
3. **Commit** — resolvers submit `keccak256(outcome, salt, address)` with collateral after resolution time
4. **Reveal** — resolvers reveal their actual vote + salt after commit deadline
5. **Finalize** — anyone calls finalize after reveal deadline; contract computes quorum outcome
6. **Claim** — winners claim proportional share of loser pot; winning resolvers claim slash pool share

---

## Deployment Details

| Item | Value |
|------|-------|
| Network | Ethereum Sepolia (chainId: 11155111) |
| Contract | `0xc046234aDCE2501ECEA50559D0122991D79e3AAC` |
| Etherscan | https://sepolia.etherscan.io/address/0xc046234aDCE2501ECEA50559D0122991D79e3AAC#code |
| Frontend | https://dmblock-assignment2.vercel.app |
| GitHub | https://github.com/xmiklosz/dmblock-assignment2 |

---

## Setup Instructions

### Prerequisites
- Node.js v20+
- MetaMask browser extension
- Sepolia ETH (get from https://sepoliafaucet.com)

### Clone and install

```bash
git clone https://github.com/xmiklosz/dmblock-assignment2.git
cd dmblock-assignment2
npm install
cd frontend && npm install
```

### Environment variables

Create `.env` in the project root:
```
PRIVATE_KEY=your_wallet_private_key
SEPOLIA_RPC_URL=https://eth-sepolia.g.alchemy.com/v2/your_alchemy_key
ETHERSCAN_API_KEY=your_etherscan_api_key
```

Create `frontend/.env`:
```
VITE_CONTRACT_ADDRESS=0xc046234aDCE2501ECEA50559D0122991D79e3AAC
VITE_RPC_URL=https://eth-sepolia.g.alchemy.com/v2/your_alchemy_key
```

### Run locally

```bash
# Run frontend
cd frontend
npm run dev
# Open http://localhost:5173
```

### Run tests

```bash
npx hardhat test
```

### Deploy

```bash
npx hardhat run scripts/deploy.ts --network sepolia
```

---

## Testing

30 tests covering all critical paths:

- Market creation (valid + 6 failure cases)
- Staking (valid + 3 failure cases)
- Commit-reveal (10 cases including double commit, wrong salt, wrong outcome, early reveal)
- Finalization and payouts (YES wins, double-claim rejection, INVALID with quorum not met, tie fallback)
- Helper functions (computeCommitment, getResolvers)

Run with:
```bash
npx hardhat test
```

---

## Bonus Points Achieved

- ✅ **Hosted public frontend** — https://dmblock-assignment2.vercel.app
- ✅ **Advanced testing** — 30 tests, all critical paths + edge cases covered

---

## Use of AI Tools

Claude (Anthropic) was used extensively throughout this project:
- Generating the initial smart contract architecture and Solidity code
- Writing the full test suite
- Scaffolding the React + TypeScript frontend components
- Debugging deployment issues and configuration errors
- Writing this README

All generated code was reviewed, understood, and tested by the team. During the presentation, all design decisions can be explained and defended.

---

## Known Limitations

- **No time-travel testing** — tests use `evm_increaseTime` but real-world timing edge cases around block timestamps are not fully covered
- **Resolver incentive balance** — the slash/reward ratio is fixed; a dynamic mechanism based on market size would be more robust
- **No frontend error boundaries** — some edge cases (e.g. MetaMask rejection mid-flow) don't surface clean error messages
- **Single contract** — a factory pattern would allow deploying isolated market instances rather than one contract managing all markets

---

## What We Learned

- Commit-reveal schemes require careful state machine design — the ordering of phases (STAKING → COMMITTING → REVEALING) must be enforced at every entry point
- `viaIR: true` is necessary for complex contracts that exceed the stack depth limit
- Ethers.js v6 has breaking changes from v5 — `provider.getContractAt` and event filtering work differently
- Vercel deployments require the root directory to be set correctly when the frontend lives in a subdirectory

## Conclusion

CommitMarket demonstrates that trustless prediction market resolution is achievable without any oracle dependency, using only economic incentives and cryptographic commitments. The commit-reveal mechanism solves the front-running problem that plagues naive on-chain voting, making resolution credible and manipulation-resistant.
