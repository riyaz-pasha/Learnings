class KeysAndRooms {
    public boolean canVisitAllRooms(List<List<Integer>> rooms) {
        boolean[] visited = new boolean[rooms.size()];
        dfs(rooms, 0, visited);
        for (boolean isVisited : visited) {
            if (!isVisited)
                return false;
        }
        return true;
    }

    private void dfs(List<List<Integer>> rooms, int room, boolean[] visited) {
        if (visited[room])
            return;
        visited[room] = true;
        for (int key : rooms.get(room)) {
            dfs(rooms, key, visited);
        }
    }
}
