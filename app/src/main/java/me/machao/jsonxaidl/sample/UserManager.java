package me.machao.jsonxaidl.sample;

/**
 * Date  2019/1/29
 *
 * @author charliema
 */
public class UserManager implements IUserManager{

    private static class Holder {
        static UserManager instance = new UserManager();
    }

    /**
     * 要使用 JSONxAIDL ，需要有一个 static 的 getInstance 方法
     * @return UserManager
     */
    public static UserManager getInstance() {
        return Holder.instance;
    }

    private User user;

    private UserManager() {

    }

    @Override
    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public User getUser() {
        return user;
    }


}
