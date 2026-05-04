# OptiMarket — Optimistic Oracle Prediction Market dApp

> **Course:** Digital Currencies and Blockchain (DMBLOCK) — Assignment 2  
> **Team:** xmiklosz  
> **Deadline:** 10 May 2026

---

## Project Description

OptiMarket is a decentralized prediction market with an **optimistic oracle resolution system**. Users create binary YES/NO markets, stake ETH on outcomes, and resolution is handled by a permissionless staked oracle network — no Chainlink or external data feed required.

The core novelty is the two-layer resolution mechanism:

1. **Optimistic proposal** — after the trading deadline, anyone can propose an outcome by posting a bond. If undisputed within the dispute window, the proposal stands automatically.
2. **Dispute & oracle vote** — if disputed, registered oracles vote with weight proportional to their staked ETH. The oracle majority decides the final outcome. The losing side (proposer or disputer) forfeits their bond to the winner.
3. **Staked oracle network** — anyone can register as an oracle by staking ≥ 0.05 ETH. Oracles that vote with the losing minority are slashed. Oracles slashed below the minimum stake are automatically deactivated.

This design is genuinely novel: it combines optimistic execution (cheap, fast resolution when there's no dispute) with decentralized human arbitration (oracle vote on contested markets), without any external oracle dependency.

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
│   State machine: Trading → Proposed → Disputed      │
│                → Resolved | Expired                 │
│                                                     │
│   - Bond-based proposal + dispute system            │
│   - Oracle vote weight = stake snapshot             │
│   - ReentrancyGuard, CEI pattern                    │
└──────────────────────┬──────────────────────────────┘
                       │ approveSlasher
┌──────────────────────▼──────────────────────────────┐
│              OracleRegistry.sol                     │
│   - Permissionless oracle registration              │
│   - Stake-weighted voting                           │
│   - Auto-deactivation on slash below MIN_STAKE      │
└─────────────────────────────────────────────────────┘
```

### Market Lifecycle

1. **Create** — creator posts question + trading deadline + bond amounts
2. **Trade** — users stake ETH on YES or NO before trading deadline
3. **Propose** — anyone posts a bond and proposes an outcome after deadline
4. **Dispute** (optional) — anyone disputes the proposal by matching the bond
5. **Oracle vote** — if disputed, registered oracles vote; majority wins
6. **Finalize** — anyone calls finalize after vote window; outcome locked
7. **Claim** — winners claim proportional share of loser pool

---

## Deployment Details

| Item | Value |
|------|-------|
| Network | Ethereum Sepolia (chainId: 11155111) |
| PredictionMarket | `0x09AE462210c42066E3eBc016BE064594Ecc48B61` |
| OracleRegistry | `0x8A999a691474f9dF6e1C25883116869852d2ED08` |
| PredictionMarket (Etherscan) | https://sepolia.etherscan.io/address/0x09AE462210c42066E3eBc016BE064594Ecc48B61#code |
| OracleRegistry (Etherscan) | https://sepolia.etherscan.io/address/0x8A999a691474f9dF6e1C25883116869852d2ED08#code |
| Frontend | https://dmblock-assignment2.vercel.app |
| GitHub | https://github.com/xmiklosz/dmblock-assignment2 |

---

## Setup Instructions

### Prerequisites
- Node.js v20+
- MetaMask browser extension
- Sepolia ETH (get from https://cloud.google.com/application/web3/faucet/ethereum/sepolia)

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
VITE_CONTRACT_ADDRESS=0x09AE462210c42066E3eBc016BE064594Ecc48B61
VITE_ORACLE_ADDRESS=0x8A999a691474f9dF6e1C25883116869852d2ED08
VITE_RPC_URL=https://eth-sepolia.g.alchemy.com/v2/your_alchemy_key
```

### Run locally

```bash
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

23 tests covering all critical paths:

- OracleRegistry: registration, minimum stake rejection, top-up/unregister, slasher approval, slash access control
- PredictionMarket creation and staking (valid + failure cases)
- Propose/dispute lifecycle: undisputed proposal, disputed with oracle flip, disputed proposer wins, non-oracle vote rejection, double vote rejection, vote outside window
- Expiry and edge cases: no proposal by deadline → INVALID, double finalize rejection, claim before finalize, no-vote dispute → proposed outcome stands, oracle deactivation after slash

Run with:
```bash
npx hardhat test
```

---

## Bonus Points Achieved

- ✅ **Hosted public frontend** — https://dmblock-assignment2.vercel.app
- ✅ **Advanced testing** — 23 tests covering all critical paths and edge cases

---

## Use of AI Tools

Claude (Anthropic) was used extensively throughout this project:
- Designing the optimistic oracle architecture and state machine
- Generating Solidity contracts (`PredictionMarket.sol`, `OracleRegistry.sol`)
- Writing the full test suite
- Scaffolding the React + TypeScript frontend
- Debugging deployment, verification, and Vercel build issues
- Writing this README

All generated code was reviewed, understood, and tested. During the presentation, all design decisions can be explained and defended.

---

## Known Limitations

- **Oracle collusion** — a majority of staked oracles could collude to resolve incorrectly; a larger, more decentralized oracle set would mitigate this
- **No frontend error boundaries** — some MetaMask rejection edge cases don't surface clean error messages
- **Fixed bond amounts** — bond sizes are hardcoded; a dynamic system based on market size would be more robust
- **Single deployment** — a factory pattern would allow isolated market instances

---

## What We Learned

- Optimistic execution patterns (propose → dispute window → finalize) are powerful for reducing on-chain costs while maintaining security
- Stake-weighted voting requires snapshotting vote weight at commit time, not at finalization — otherwise slashing can retroactively break reward math
- `viaIR: true` is required for contracts with deep stack usage
- Vercel deployments with a frontend subdirectory require careful root directory and build command configuration

## Conclusion

OptiMarket demonstrates a novel combination of optimistic execution and decentralized oracle arbitration for prediction market resolution. The two-layer system (cheap undisputed path + expensive but secure disputed path) mirrors real-world optimistic rollup designs, making it both practically efficient and theoretically sound.
