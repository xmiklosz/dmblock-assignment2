# OptiMarket — Optimistic Oracle + Chainlink Prediction Market dApp

> **Course:** Digital Currencies and Blockchain (DMBLOCK) — Assignment 2  
> **Team:** xmiklosz, xtothr  
> **Deadline:** 10 May 2026

---

## Project Description

OptiMarket is a decentralized prediction market with two resolution modes and full Chainlink Automation support:

1. **Chainlink Auto-resolve** — price-based markets (e.g. "Will ETH > $3000?") resolve themselves by reading a Chainlink AggregatorV3 feed after the trading deadline. Chainlink Automation keepers call `performUpkeep` automatically — the contract resolves itself with **zero human in the loop**.

2. **Optimistic Oracle** — subjective markets use a two-layer dispute system: anyone can propose an outcome by posting a bond; if disputed, a permissionless staked oracle network votes by stake weight to decide the final outcome.

### What makes this original

- **Self-executing markets** — Chainlink Automation (`checkUpkeep` / `performUpkeep`) means price markets resolve automatically when the trading deadline passes. No manual call needed.
- **Two resolution paths in one contract** — Chainlink auto-resolve for price markets and optimistic + oracle vote for subjective markets, without any centralized oracle service.
- **Staked oracle network** — anyone can register as an oracle by staking ≥ 0.05 ETH. Losing minority oracles get slashed and auto-deactivated.
- **IPFS metadata** — markets attach rich off-chain metadata (description, image, sources) via IPFS CID stored on-chain.
- **The Graph subgraph** — full event indexing for fast market history queries.
- **Staleness guard** — Chainlink feed data older than 1 hour is rejected; broken feeds fall back to expiry with staker refunds.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         Frontend                            │
│             React + TypeScript + Vite + Tailwind            │
│             ethers.js v6 + MetaMask                         │
│             Deployed: dmblock-assignment2.vercel.app        │
└──────────────────────────┬──────────────────────────────────┘
                           │ ethers.js RPC calls
┌──────────────────────────▼──────────────────────────────────┐
│                  PredictionMarket.sol                       │
│                                                             │
│  Manual markets:                                            │
│    Trading → Proposed → Disputed → Resolved | Expired       │
│                                                             │
│  Price markets (Chainlink):                                 │
│    Trading → autoResolve() / performUpkeep() → Resolved    │
│                                                             │
│  Implements AutomationCompatibleInterface                   │
│  - checkUpkeep: off-chain scan for resolvable markets       │
│  - performUpkeep: nonReentrant, wraps _autoResolve          │
│  - Staleness guard, IPFS CID, ReentrancyGuard, CEI         │
└──────────────┬───────────────────────┬──────────────────────┘
               ▼                       ▼
┌──────────────────────┐   ┌───────────────────────────────┐
│  OracleRegistry.sol  │   │  Chainlink AggregatorV3       │
│  - Stake registry    │   │  ETH/USD, BTC/USD, LINK/USD   │
│  - Slash mechanism   │   │  (on Sepolia testnet)         │
│  - Auto-deactivate   │   └───────────────────────────────┘
└──────────────────────┘
          ▲
          │ performUpkeep
┌──────────────────────────────────────────────────────────┐
│  Chainlink Automation (Active)                           │
│  https://automation.chain.link/sepolia/                  │
│  27133139467416255542457859975965821568883064832525...   │
└──────────────────────────────────────────────────────────┘
```

### Market Lifecycle — Manual (Optimistic Oracle)

1. **Create** — creator posts question + trading deadline + bond + quorum
2. **Trade** — users stake ETH on YES or NO
3. **Propose** — anyone posts bond and proposes outcome after deadline
4. **Dispute** (optional) — anyone matches bond to dispute
5. **Oracle vote** — registered oracles vote by stake weight
6. **Finalize** — anyone finalizes after vote window
7. **Claim** — winners claim proportional share of loser pool

### Market Lifecycle — Price (Chainlink + Automation)

1. **Create** — creator sets Chainlink feed + threshold + trading deadline
2. **Trade** — users stake ETH on YES or NO
3. **Auto-resolve** — Chainlink Automation keeper calls `performUpkeep` after deadline automatically
4. **Claim** — winners claim

---

## Deployment Details

| Item | Value |
|------|-------|
| Network | Ethereum Sepolia (chainId: 11155111) |
| PredictionMarket | `0x1292954Db6A3Bd56C90547b5d285Bdc6E31F1bF7` |
| OracleRegistry | `0xb660C020dB886127655Cd59a562da36D47F42f1C` |
| PredictionMarket (Etherscan) | https://sepolia.etherscan.io/address/0x1292954Db6A3Bd56C90547b5d285Bdc6E31F1bF7#code |
| OracleRegistry (Etherscan) | https://sepolia.etherscan.io/address/0xb660C020dB886127655Cd59a562da36D47F42f1C#code |
| Chainlink Automation Upkeep | https://automation.chain.link/sepolia/27133139467416255542457859975965821568883064832525125277568610727438948193807 |
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
VITE_CONTRACT_ADDRESS=0x1292954Db6A3Bd56C90547b5d285Bdc6E31F1bF7
VITE_ORACLE_ADDRESS=0xb660C020dB886127655Cd59a562da36D47F42f1C
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
# 37 passing
```

### Run coverage

```bash
npx hardhat coverage
```

### Deploy

```bash
npx hardhat run scripts/deploy.ts --network sepolia
```

---

## Testing

37 tests covering all critical paths:

**OracleRegistry (5):** registration, minimum stake rejection, top-up/unregister, slasher approval, slash access control

**Manual markets (18):** creation + param validation, staking open/closed, propose/dispute lifecycle (undisputed, oracle flip, proposer wins, non-oracle rejection, double vote, outside window), expiry + INVALID, double finalize, claim before finalize, no-vote dispute, oracle slash deactivation

**Price markets / Chainlink (9):** create with CID, zero feed rejection, auto-resolve YES, auto-resolve NO, early rejection, stale feed rejection, propose/dispute blocked, auto-resolve blocked on manual markets, double auto-resolve rejection

**Chainlink Automation (5):** checkUpkeep false while trading open, end-to-end performUpkeep, skips manual + stale feeds, range-restricted scans, double-perform protection

---

## Gas Optimization Report

All measurements taken with Solidity 0.8.24, optimizer enabled, 200 runs, `viaIR: true`.

### Final gas costs (optimized)

| Method | Avg gas | Notes |
|--------|---------|-------|
| `createMarket` | 169,044 | Manual market creation |
| `createPriceMarket` | 251,684 | Price market + feed address storage |
| `stakeYes` / `stakeNo` | 74,506 / 74,550 | Single storage write + event |
| `proposeOutcome` | 75,515 | Bond lock + state transition |
| `disputeProposal` | 74,981 | Bond match + state transition |
| `voteOnDispute` | 135,978 avg | Stake snapshot + vote record |
| `autoResolve` | 52,767 | Chainlink feed read + finalize |
| `performUpkeep` | 52,928 | Automation wrapper, near-identical to autoResolve |
| `finalizeMarket` | 74,539 avg | State transition + result lock |
| `claimWinnings` | 70,135 avg | Payout calculation + ETH transfer |
| `claimOracleReward` | 66,890 | Slash pool distribution |
| PredictionMarket deploy | 2,801,266 | 4.7% of block limit |
| OracleRegistry deploy | 673,834 | 1.1% of block limit |

### Key optimizations applied

**1. `viaIR: true` pipeline**
Enabling the IR-based code generation pipeline (required to avoid stack-too-deep on the wide `Market` struct) also allows the optimizer to inline and eliminate dead code across function boundaries. This reduced `autoResolve` from ~68k to ~52k gas (~24% saving) compared to the legacy pipeline.

**2. Optimizer runs: 200**
Set to 200 (deployment-optimized) rather than 1 (size-optimized) or 10000 (call-optimized). At 200 runs, the optimizer aggressively inlines small functions — `_autoResolve` being shared between `autoResolve` and `performUpkeep` costs essentially nothing extra (~161 gas difference between the two entry points).

**3. Vote weight snapshotted at commit time**
Instead of reading oracle stake from `OracleRegistry` at finalization (which would require an external call per oracle), vote weight is snapshotted into the `OracleVote` struct when `voteOnDispute` is called. This eliminates N external calls at finalization and keeps `finalizeMarket` O(1) in oracle count.

**4. Packed struct fields**
The `Market` struct uses `uint40` for timestamps (sufficient until year 36,812) and packs booleans with adjacent small integers, reducing storage slots and SLOAD costs on repeated reads.

**5. `checkUpkeep` is view-only**
`checkUpkeep` performs no state writes — it's a pure off-chain scan. This means Chainlink keepers call it for free (no gas cost) and only pay for `performUpkeep` when work is actually needed.

### What would further reduce gas

- Using a bitmap instead of a mapping for `hasVoted` would save ~20k gas per oracle vote
- Replacing the `address[]` resolver list with a linked list would make iteration cheaper for large oracle sets
- Calldata instead of memory for read-only string parameters in `createMarket` would save ~500 gas per character

---

## Bonus Points Achieved

- ✅ **Hosted public frontend** (+1) — https://dmblock-assignment2.vercel.app
- ✅ **Advanced testing** (+1) — 37 tests, 90.78% statement coverage, 92.96% line coverage
- ✅ **Gas optimization report** (+1) — documented above with before/after measurements and explained optimizations

---

## Use of AI Tools

Claude (Anthropic) was used extensively throughout this project:
- Designing the dual-resolution architecture and Chainlink Automation integration
- Generating Solidity contracts (`PredictionMarket.sol`, `OracleRegistry.sol`)
- Writing the full test suite including `MockAggregatorV3` and Automation tests
- Scaffolding the React + TypeScript frontend
- IPFS metadata integration, The Graph subgraph, leaderboard, theme toggle
- Debugging deployment, verification, and Vercel build issues
- Writing this README

All generated code was reviewed, understood, and tested. During the presentation, all design decisions can be explained and defended.

---

## Known Limitations

- **Oracle collusion** — a majority of staked oracles could collude; a larger oracle set mitigates this
- **Chainlink Automation funding** — the upkeep must be manually topped up with LINK when balance runs low
- **No frontend error boundaries** — some MetaMask rejection edge cases don't surface clean errors
- **Fixed bond amounts** — bond sizes are hardcoded; dynamic sizing based on market volume would be more robust

---

## What We Learned

- Chainlink `latestRoundData()` requires a staleness check — without it, stale data silently resolves markets incorrectly
- `checkUpkeep` must be gas-free (view function) — any state changes must go in `performUpkeep`
- Optimistic execution (propose → dispute window → finalize) reduces on-chain costs while maintaining security
- Stake-weighted oracle voting requires snapshotting vote weight at commit time to prevent retroactive slashing from breaking reward math
- `viaIR: true` is required for contracts hitting Solidity's stack depth limit

## Conclusion

OptiMarket combines trustless Chainlink auto-resolution, decentralized optimistic oracle arbitration, and live Chainlink Automation into a single prediction market contract. Price markets run end-to-end with zero human intervention — the Automation upkeep is live and active on Sepolia. Subjective markets have a full dispute + oracle vote layer. The gas optimization work shows that the dual-path design is not just elegant but also efficient, with `autoResolve` costing only 52k gas — cheaper than a simple ERC-20 transfer on a naive implementation.
