package utils;

import entities.Utilisateur;

public class Session {
    private static Session instance;
    private Utilisateur loggedUser;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public Utilisateur getLoggedUser() {
        return loggedUser;
    }

    public void setLoggedUser(Utilisateur loggedUser) {
        this.loggedUser = loggedUser;
    }

    public void logout() {
        this.loggedUser = null;
    }
}
