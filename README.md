# OptiMarket — Optimistic Oracle + Chainlink Prediction Market dApp

> **Course:** Digital Currencies and Blockchain (DMBLOCK) — Assignment 2  
> **Team:** xmiklosz  
> **Deadline:** 10 May 2026

---

## Project Description

OptiMarket is a decentralized prediction market with two resolution modes:

1. **Chainlink Auto-resolve** — price-based markets (e.g. "Will ETH > $3000?") resolve themselves by reading a Chainlink AggregatorV3 feed after the trading deadline. No oracle vote needed, no human intervention, fully trustless.

2. **Optimistic Oracle** — subjective markets use a two-layer dispute system: anyone can propose an outcome by posting a bond; if disputed, a permissionless staked oracle network votes by stake weight to decide the final outcome.

### What makes this original

- **Two resolution paths in one contract** — Chainlink auto-resolve for price markets (cheap, instant, trustless) and optimistic + oracle vote for subjective markets (decentralized human arbitration). Neither path requires a centralized oracle service.
- **Staked oracle network** — anyone can register as an oracle by staking ≥ 0.05 ETH. Oracles voting with the losing minority get slashed and auto-deactivated below minimum stake.
- **IPFS metadata** — markets can attach rich off-chain metadata (description, image, sources) via IPFS CID stored on-chain.
- **The Graph subgraph** — full event indexing for fast market history queries without scanning all blocks.
- **Staleness guard** — Chainlink feed data older than 1 hour is rejected; broken feeds fall back to expiry with staker refunds.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                       Frontend                          │
│           React + TypeScript + Vite + Tailwind          │
│           ethers.js v6 + MetaMask                       │
│           Deployed: dmblock-assignment2.vercel.app      │
└────────────────────────┬────────────────────────────────┘
                         │ ethers.js RPC calls
┌────────────────────────▼────────────────────────────────┐
│                PredictionMarket.sol                     │
│                                                         │
│  Manual markets:                                        │
│    Trading → Proposed → Disputed → Resolved | Expired   │
│                                                         │
│  Price markets (Chainlink):                             │
│    Trading → autoResolve() → Resolved | Expired        │
│                                                         │
│  - Bond-based proposal + dispute                        │
│  - Chainlink AggregatorV3 + staleness guard             │
│  - IPFS metadata CID stored on-chain                    │
│  - ReentrancyGuard, CEI pattern                         │
└────────────────────────┬────────────────────────────────┘
              ┌──────────┴──────────┐
              ▼                     ▼
┌─────────────────────┐   ┌─────────────────────────────┐
│  OracleRegistry.sol │   │  Chainlink AggregatorV3     │
│  - Stake registry   │   │  ETH/USD, BTC/USD, etc.     │
│  - Slash mechanism  │   │  (external, on Sepolia)     │
│  - Auto-deactivate  │   └─────────────────────────────┘
└─────────────────────┘
```

### Market Lifecycle — Manual (Optimistic Oracle)

1. **Create** — creator posts question + trading deadline + bond + quorum
2. **Trade** — users stake ETH on YES or NO
3. **Propose** — anyone posts bond and proposes outcome after deadline
4. **Dispute** (optional) — anyone matches bond to dispute
5. **Oracle vote** — registered oracles vote by stake weight
6. **Finalize** — anyone finalizes after vote window
7. **Claim** — winners claim proportional share of loser pool

### Market Lifecycle — Price (Chainlink)

1. **Create** — creator sets Chainlink feed address + threshold + trading deadline
2. **Trade** — users stake ETH on YES or NO
3. **autoResolve()** — anyone calls after trading deadline; contract reads `latestRoundData()`, checks staleness, resolves YES/NO in one tx
4. **Claim** — winners claim

---

## Deployment Details

| Item | Value |
|------|-------|
| Network | Ethereum Sepolia (chainId: 11155111) |
| PredictionMarket | `0x3e33743DD0eAcda1526b6674beC2EDA53e0059B0` |
| OracleRegistry | `0x3Ad194b9dADF0fD6459A4849CB8B02A5985528bB` |
| PredictionMarket (Etherscan) | https://sepolia.etherscan.io/address/0x3e33743DD0eAcda1526b6674beC2EDA53e0059B0#code |
| OracleRegistry (Etherscan) | https://sepolia.etherscan.io/address/0x3Ad194b9dADF0fD6459A4849CB8B02A5985528bB#code |
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
VITE_CONTRACT_ADDRESS=0x3e33743DD0eAcda1526b6674beC2EDA53e0059B0
VITE_ORACLE_ADDRESS=0x3Ad194b9dADF0fD6459A4849CB8B02A5985528bB
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

32 tests covering all critical paths:

**OracleRegistry (5 tests):** registration, minimum stake rejection, top-up/unregister, slasher approval, slash access control

**PredictionMarket — manual markets (18 tests):** creation + param validation, staking open/closed, propose/dispute lifecycle (undisputed, oracle flip, proposer wins, non-oracle rejection, double vote, outside window), expiry + INVALID, double finalize, claim before finalize, no-vote dispute, oracle slash deactivation

**PredictionMarket — price markets / Chainlink (9 tests):** create with CID, zero feed rejection, auto-resolve YES, auto-resolve NO, early auto-resolve rejection, stale feed rejection, propose/dispute blocked on price markets, auto-resolve blocked on manual markets, double auto-resolve rejection

Run with:
```bash
npx hardhat test
# 32 passing
```

---

## Bonus Points Achieved

- ✅ **Hosted public frontend** — https://dmblock-assignment2.vercel.app
- ✅ **Advanced testing** — 32 tests, all critical paths + edge cases + Chainlink mock

---

## Use of AI Tools

Claude (Anthropic) was used extensively throughout this project:
- Designing the dual-resolution architecture (Chainlink + optimistic oracle)
- Generating Solidity contracts (`PredictionMarket.sol`, `OracleRegistry.sol`)
- Writing the full test suite including `MockAggregatorV3`
- Scaffolding the React + TypeScript frontend with Chainlink/manual toggle
- IPFS metadata integration and The Graph subgraph
- Debugging deployment, verification, and Vercel build issues
- Writing this README

All generated code was reviewed, understood, and tested. During the presentation, all design decisions can be explained and defended.

---

## Known Limitations

- **Oracle collusion** — a majority of staked oracles could collude to resolve incorrectly; a larger oracle set mitigates this
- **Chainlink feed availability** — only feeds available on Sepolia are supported; mainnet has more options
- **No frontend error boundaries** — some MetaMask rejection edge cases don't surface clean error messages
- **Fixed bond amounts** — bond sizes are hardcoded; dynamic sizing based on market volume would be more robust

---

## What We Learned

- Chainlink `latestRoundData()` requires a staleness check — stale data can silently resolve markets incorrectly without it
- Optimistic execution (propose → dispute window → finalize) is a powerful pattern for reducing on-chain costs while maintaining security
- Stake-weighted oracle voting requires snapshotting vote weight at commit time to prevent retroactive slashing from breaking reward math
- `viaIR: true` is required for contracts with deep stack usage in Solidity 0.8.24
- Vercel deployments with a frontend subdirectory require careful root directory and build command configuration

## Conclusion

OptiMarket combines two resolution paradigms in a single contract: trustless Chainlink auto-resolution for price markets and decentralized optimistic oracle arbitration for subjective markets. The result is a prediction market that is both cheap to operate (most price markets resolve in one tx with no oracle participation) and robust (subjective markets have a full dispute + vote layer). The dual-path design is the core original contribution of this project.
