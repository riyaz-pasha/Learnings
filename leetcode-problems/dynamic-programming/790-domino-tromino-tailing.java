class DominoAndTrominoTiling {

    public int numTilings(int n) {
        final int MOD = 1_000_000_007;

        if (n == 1)
            return 1;
        if (n == 2)
            return 2;
        if (n == 3)
            return 5;

        long[] dp = new long[n + 1];
        dp[0] = 1;
        dp[1] = 1;
        dp[2] = 2;
        dp[3] = 5;

        for (int i = 4; i <= n; i++) {
            long verticalDominoOrTrominoExtension = dp[i - 1]; // covers vertical and part of tromino mirror logic
            long trominoShapes = dp[i - 3]; // trominoes that start fresh from 3-wide section

            dp[i] = (2 * verticalDominoOrTrominoExtension % MOD + trominoShapes) % MOD;
        }

        return (int) dp[n];
    }

}

class DominoAndTrominoTiling2 {

    final int MOD = 1_000_000_007;
    Long[][] memo;

    public int numTilings(int n) {
        // memo[i][0] = number of ways to tile from column i with no overhang
        // memo[i][1] = number of ways to tile from column i with overhang
        memo = new Long[n + 1][2];
        return (int) countTilings(0, n, false);
    }

    private long countTilings(int col, int n, boolean hasOverhang) {
        if (col == n) {
            return hasOverhang ? 0 : 1; // valid tiling only if no overhang left
        }
        if (col > n)
            return 0;

        int overhangFlag = hasOverhang ? 1 : 0;
        if (memo[col][overhangFlag] != null) {
            return memo[col][overhangFlag];
        }

        long totalWays = 0;

        if (hasOverhang) {
            // Complete the overhang in two ways:
            long fixWithHorizontalDomino = countTilings(col + 1, n, false);
            long extendOverhangWithTromino = countTilings(col + 1, n, true);
            totalWays = (fixWithHorizontalDomino + extendOverhangWithTromino) % MOD;
        } else {
            // 1. Place vertical domino
            long placeVerticalDomino = countTilings(col + 1, n, false);

            // 2. Place two horizontal dominoes
            long placeTwoHorizontals = countTilings(col + 2, n, false);

            // 3. Place L-tromino (two orientations), causing overhang
            long placeTrominoWithOverhang = (2 * countTilings(col + 2, n, true)) % MOD;

            totalWays = (placeVerticalDomino + placeTwoHorizontals + placeTrominoWithOverhang) % MOD;
        }

        memo[col][overhangFlag] = totalWays;
        return totalWays;
    }

}

class DominoAndTrominoTiling3 {

    private static final int MOD = 1_000_000_007;
    private Long[][] memo;

    public int numTilings(int n) {
        // memo[col][0] = no overhang at column `col`
        // memo[col][1] = overhang exists at column `col`
        memo = new Long[n + 1][2];
        return (int) countTilingsFrom(0, false, n);
    }

    private long countTilingsFrom(int col, boolean hasOverhang, int boardWidth) {
        if (col == boardWidth)
            return hasOverhang ? 0 : 1;
        if (col > boardWidth)
            return 0;

        int overhangState = hasOverhang ? 1 : 0;
        if (memo[col][overhangState] != null)
            return memo[col][overhangState];

        long ways;
        if (hasOverhang) {
            ways = handleOverhangAt(col, boardWidth);
        } else {
            ways = handleFullyAlignedAt(col, boardWidth);
        }

        return memo[col][overhangState] = ways;
    }

    // 🧩 When there's an overhang, fix it with a horizontal domino or extend it
    // with a mirrored tromino
    private long handleOverhangAt(int col, int n) {
        long fixWithHorizontalDomino = countTilingsFrom(col + 1, false, n);
        long extendWithMirroredTromino = countTilingsFrom(col + 1, true, n);
        return (fixWithHorizontalDomino + extendWithMirroredTromino) % MOD;
    }

    // 🧩 When perfectly aligned, explore all placements: vertical domino,
    // horizontal pair, or tromino
    private long handleFullyAlignedAt(int col, int n) {
        long placeVerticalDomino = countTilingsFrom(col + 1, false, n);
        long placeTwoHorizontalDominoes = countTilingsFrom(col + 2, false, n);
        long placeLShapedTromino = (2 * countTilingsFrom(col + 2, true, n)) % MOD;

        return (placeVerticalDomino + placeTwoHorizontalDominoes + placeLShapedTromino) % MOD;
    }

}
