package com.teaverse.compensation.model;

import java.util.ArrayList;
import java.util.List;

public class GameProfile {
    private String mainGame;
    private String rank;
    private String goal;
    private String preferredRole;
    private String onlineTime;
    private List<String> favoriteGames = new ArrayList<>();

    public String getMainGame() {
        return mainGame;
    }

    public void setMainGame(String mainGame) {
        this.mainGame = mainGame;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getPreferredRole() {
        return preferredRole;
    }

    public void setPreferredRole(String preferredRole) {
        this.preferredRole = preferredRole;
    }

    public String getOnlineTime() {
        return onlineTime;
    }

    public void setOnlineTime(String onlineTime) {
        this.onlineTime = onlineTime;
    }

    public List<String> getFavoriteGames() {
        return favoriteGames;
    }

    public void setFavoriteGames(List<String> favoriteGames) {
        this.favoriteGames = favoriteGames == null ? new ArrayList<>() : favoriteGames;
    }
}
