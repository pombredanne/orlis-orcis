package org.softeg.slartus.forpdaplus.classes.forum;

import android.text.TextUtils;

import org.softeg.slartus.forpdaapi.Topic;
import org.softeg.slartus.forpdaplus.Client;
import org.softeg.slartus.forpdaplus.classes.ForumItem;
import org.softeg.slartus.forpdaplus.classes.IListItem;
import org.softeg.slartus.forpdaplus.fragments.topic.ThemeFragment;

import java.io.Serializable;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Admin
 * Date: 19.09.11
 * Time: 20:46
 * To change this template use File | Settings | File Templates.
 */
public class ExtTopic extends Topic implements ForumItem, IListItem, Serializable {



    private String authKey;


    public ExtTopic(Topic topic) {

        setId(topic.getId());
        setTitle(topic.getTitle(), false);

        setIsNew(topic.getIsNew());
        setDescription(topic.getDescription(), false);
        setLastMessageDate(topic.getLastMessageDate());
        setLastMessageAuthor(topic.getLastMessageAuthor(), false);
    }

    public ExtTopic(String id, String title) {
        super(id, title);
    }

    private int m_PagesCount = 1;

    public int getPagesCount() {
        return m_PagesCount;
    }

    public void setPagesCount(String value) {
        m_PagesCount = Integer.parseInt(value) + 1;
    }

    public int getPostsPerPageCount(String lastUrl) {
if(m_PagesCount==1)
    return 0;
        URI redirectUri = Client.getInstance().getRedirectUri();
        if (redirectUri != null)
            lastUrl = redirectUri.toString();
        Pattern p = Pattern.compile("st=(\\d+)");
        Matcher m = p.matcher(lastUrl);
        if (m.find())
            m_LastPageStartCount = Math.max(Integer.parseInt(m.group(1)), m_LastPageStartCount);

        return m_LastPageStartCount / (m_PagesCount - 1);
    }

    private int m_LastPageStartCount = 0;

    public void setLastPageStartCount(String value) {
        m_LastPageStartCount = Math.max(Integer.parseInt(value), m_LastPageStartCount);
    }

    private int m_CurrentPage = 0;

    public void setCurrentPage(String value) {
        m_CurrentPage = Integer.parseInt(value);
    }

    public int getCurrentPage() {
        return m_CurrentPage;
    }

    public void showActivity() {
        ThemeFragment.showTopicById(m_Id);
    }

    public void showActivity(String params) {
        showActivity(m_Id, params);
    }

    public static void showActivity(CharSequence themeId, CharSequence params) {
        ThemeFragment.showTopicById(themeId, params);
    }
    public static void showActivity(CharSequence title, CharSequence themeId, CharSequence params) {
        ThemeFragment.showTopicById(title, themeId, params);
    }

    public String getShowBrowserUrl(String params) {
        return getShowBrowserUrl(m_Id, params);
    }

    public static String getShowBrowserUrl(CharSequence id, CharSequence params) {
        return "http://4pda.ru/forum/index.php?showtopic=" + id + (TextUtils.isEmpty(params) ? "" : ("&" + params));
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public boolean getIsOld() {
        return false;
    }

    public void dispose() {

    }

    private boolean postVote = true;
    public void setPostVote(boolean b){
        postVote = b;
    }
    public boolean isPostVote(){
        return postVote;
    }

}
