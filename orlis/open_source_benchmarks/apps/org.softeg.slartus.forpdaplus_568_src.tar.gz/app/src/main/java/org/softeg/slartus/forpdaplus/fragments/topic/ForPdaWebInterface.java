package org.softeg.slartus.forpdaplus.fragments.topic;

/**
 * Created by radiationx on 28.10.15.
 */

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.softeg.slartus.forpdaplus.Client;
import org.softeg.slartus.forpdaplus.MainActivity;
import org.softeg.slartus.forpdaplus.R;
import org.softeg.slartus.forpdaplus.classes.ForumUser;
import org.softeg.slartus.forpdaplus.classes.TopicAttaches;
import org.softeg.slartus.forpdaplus.common.AppLog;
import org.softeg.slartus.forpdaplus.download.DownloadsService;
import org.softeg.slartus.forpdaplus.listfragments.TopicReadersListFragment;
import org.softeg.slartus.forpdaplus.listfragments.TopicWritersListFragment;
import org.softeg.slartus.forpdaplus.listfragments.next.UserReputationFragment;
import org.softeg.slartus.forpdaplus.listtemplates.TopicReadersBrickInfo;
import org.softeg.slartus.forpdaplus.listtemplates.TopicWritersBrickInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * Created by slinkin on 23.06.2015.
 */
public class ForPdaWebInterface {
    public static final String NAME = "HTMLOUT";
    private FragmentActivity activity;
    private ThemeFragment context;

    public ForPdaWebInterface(ThemeFragment context) {
        this.context = context;
        this.activity = context.getMainActivity();
    }

    private ThemeFragment getContext() {
        return context;
    }
    private FragmentActivity getMainActivity(){
        return activity;
    }


    @JavascriptInterface
    public void showImgPreview(final String title, final String previewUrl, final String fullUrl) {
        run(new Runnable() {
            @Override
            public void run() {
                ThemeFragment.showImgPreview(getMainActivity(), title, previewUrl, fullUrl);
            }
        });
    }

    @JavascriptInterface
    public void quote(final String forumId, final String topicId, final String postId, final String postDate, final String userId, final String userNick) {
        run(new Runnable() {
                @Override
                public void run() {
                    getContext().quote(forumId, topicId, postId, postDate, userId, userNick);

                }
            }
        );
    }

    @JavascriptInterface
    public void checkBodyAndReload(final String postBody) {
        run(new Runnable() {
            @Override
            public void run() {
                getContext().checkBodyAndReload(postBody);
            }
        });

    }

    @JavascriptInterface
    public void showTopicAttaches(final String postBody) {
        run(new Runnable() {
            @Override
            public void run() {
                final TopicAttaches topicAttaches = new TopicAttaches();
                topicAttaches.parseAttaches(postBody);
                if (topicAttaches.size() == 0) {
                    Toast.makeText(getMainActivity(), "Страница не имеет вложений", Toast.LENGTH_SHORT).show();
                    return;
                }
                final boolean[] selection = new boolean[topicAttaches.size()];
                new MaterialDialog.Builder(getMainActivity())
                        .title("Вложения")
                        .items(topicAttaches.getList())
                        .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog materialDialog, Integer[] which, CharSequence[] charSequence) {
                                for (int i = 0; i < which.length; i++) {
                                    selection[which[i]] = true;
                                }
                                return true;
                            }
                        })
                        .alwaysCallMultiChoiceCallback()
                        .positiveText("Скачать")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                if (!Client.getInstance().getLogined()) {
                                    new MaterialDialog.Builder(getMainActivity())
                                            .title("Внимание!")
                                            .content("Для скачивания файлов с сайта необходимо залогиниться!")
                                            .positiveText("ОК")
                                            .show();
                                    return;
                                }
                                for (int j = 0; j < selection.length; j++) {
                                    if (!selection[j]) continue;
                                    DownloadsService.download(((Activity)getMainActivity()), topicAttaches.get(j).getUri(), false);
                                    selection[j] = false;
                                }
                            }
                        })
                        .negativeText("Отмена")
                        .show();
            }
        });
    }

    @JavascriptInterface
    public void showPostLinkMenu(final String postId) {
        run(new Runnable() {
            @Override
            public void run() {
                getContext().showLinkMenu(org.softeg.slartus.forpdaplus.classes.Post.getLink(getContext().getTopic().getId(), postId), postId);
            }
        })
        ;
    }

    @JavascriptInterface
    public void postVoteBad(final String postId) {
        run(new Runnable() {
            @Override
            public void run() {
                new MaterialDialog.Builder(getMainActivity())
                        .title("Подтвердите действие")
                        .content("Понизить рейтинг сообщения?")
                        .positiveText("Понизить")
                        .negativeText("Отмена")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                org.softeg.slartus.forpdaplus.classes.Post.minusOne(getMainActivity(), new Handler(), postId);
                            }
                        }).show();
            }
        })
        ;
    }

    @JavascriptInterface
    public void postVoteGood(final String postId) {
        run(new Runnable() {
            @Override
            public void run() {
                new MaterialDialog.Builder(getMainActivity())
                        .title("Подтвердите действие")
                        .content("Повысить рейтинг сообщения?")
                        .positiveText("Повысить")
                        .negativeText("Отмена")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                org.softeg.slartus.forpdaplus.classes.Post.plusOne(getMainActivity(), new Handler(), postId);
                            }
                        }).show();
            }
        })
        ;
    }

    @JavascriptInterface
    public void showReadingUsers() {
        run(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle args = new Bundle();
                    args.putString(TopicReadersListFragment.TOPIC_ID_KEY, getContext().getTopic().getId());
                    MainActivity.showListFragment(getContext().getTopic().getId(), TopicReadersBrickInfo.NAME, args);
                } catch (ActivityNotFoundException e) {
                    AppLog.e(getMainActivity(), e);
                }
            }
        });

    }

    @JavascriptInterface
    public void showWriters() {
        run(new Runnable() {
            @Override
            public void run() {
                if (getContext().getTopic() == null) {
                    Toast.makeText(getMainActivity(), "Что-то пошло не так", Toast.LENGTH_SHORT).show();
                } else {
                    Bundle args = new Bundle();
                    args.putString(TopicWritersListFragment.TOPIC_ID_KEY, getContext().getTopic().getId());
                    MainActivity.showListFragment(getContext().getTopic().getId(), TopicWritersBrickInfo.NAME, args);

                }
            }
        });
    }

    @JavascriptInterface
    public void showUserMenu(final String postId, final String userId, final String userNick) {
        run(new Runnable() {
            @Override
            public void run() {
                ForumUser.showUserQuickAction(getMainActivity(), getContext().getWebView(), getContext().getTopic().getId(), postId, userId, userNick,
                        new ForumUser.InsertNickInterface() {
                            @Override
                            public void insert(String text) {
                                insertTextToPost(text);
                            }
                        }
                );
            }
        });
    }

    @JavascriptInterface
    public void insertTextToPost(final String text) {
        if(android.os.Build.VERSION.SDK_INT >= 16){
            run(new Runnable() {
                @Override
                public void run() {
                    new Handler().post(new Runnable() {
                        public void run() {
                            getContext().insertTextToPost(text);
                        }
                    });
                }
            });
        }else {
            getMainActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new Handler().post(new Runnable() {
                        public void run() {
                            getContext().insertTextToPost(text);
                        }
                    });
                }
            });
        }

    }

    @JavascriptInterface
    public void showPostMenu(final String postId, final String postDate,
                             final String userId, final String userNick,
                             final String canEdit, final String canDelete) {
        run(new Runnable() {
            @Override
            public void run() {
                getContext().openActionMenu(postId, postDate, userId, userNick, "1".equals(canEdit), "1".equals(canDelete));
            }
        });
    }

    @JavascriptInterface
    public void post() {
        run(new Runnable() {
            @Override
            public void run() {
                getContext().post();
            }
        });
    }

    @JavascriptInterface
    public void nextPage() {
        run(new Runnable() {
            @Override
            public void run() {
                getContext().nextPage();
            }
        });
    }

    @JavascriptInterface
    public void prevPage() {
        run(new Runnable() {
            @Override
            public void run() {
                getContext().prevPage();
            }
        });

    }

    @JavascriptInterface
    public void firstPage() {
        run(new Runnable() {
            @Override
            public void run() {
                getContext().firstPage();
            }
        });
    }

    @JavascriptInterface
    public void lastPage() {
        run(new Runnable() {
            @Override
            public void run() {
                getContext().lastPage();
            }
        });
    }

    @JavascriptInterface
    public void jumpToPage() {
        run(new Runnable() {
            @Override
            public void run() {
                try {

                    final CharSequence[] pages = new CharSequence[getContext().getTopic().getPagesCount()];

                    final int postsPerPage = getContext().getTopic().getPostsPerPageCount(getContext().getLastUrl());

                    for (int p = 0; p < getContext().getTopic().getPagesCount(); p++) {
                        pages[p] = "Стр. " + (p + 1) + " (" + ((p * postsPerPage + 1) + "-" + (p + 1) * postsPerPage) + ")";
                    }

                    LayoutInflater inflater = (LayoutInflater) getMainActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View view = inflater.inflate(R.layout.select_page_layout, null);

                    assert view != null;
                    final ListView listView = (ListView) view.findViewById(R.id.lstview);
                    listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getMainActivity(),
                            R.layout.simple_list_item_single_choice, pages);
                    // присваиваем адаптер списку
                    listView.setAdapter(adapter);

                    final EditText txtNumberPage = (EditText) view.findViewById(R.id.txtNumberPage);
                    txtNumberPage.setText(Integer.toString(getContext().getTopic().getCurrentPage()));
                    txtNumberPage.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                            if (txtNumberPage.getTag() != null && !((Boolean) txtNumberPage.getTag()))
                                return;
                            if (TextUtils.isEmpty(charSequence)) return;
                            try {
                                int value = Integer.parseInt(charSequence.toString());
                                value = Math.min(pages.length - 1, value - 1);
                                listView.setTag(false);
                                listView.setItemChecked(value, true);
                                listView.setSelection(value);
                            } catch (Throwable ex) {
                                AppLog.e(getMainActivity(), ex);
                            } finally {
                                listView.setTag(true);
                            }

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {

                        }
                    });

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            if (listView.getTag() != null && !((Boolean) listView.getTag()))
                                return;
                            txtNumberPage.setTag(false);
                            try {
                                txtNumberPage.setText(Integer.toString((int) l + 1));
                            } catch (Throwable ex) {
                                AppLog.e(getMainActivity(), ex);
                            } finally {
                                txtNumberPage.setTag(true);
                            }
                        }
                    });

                    listView.setItemChecked(getContext().getTopic().getCurrentPage() - 1, true);
                    listView.setSelection(getContext().getTopic().getCurrentPage() - 1);

                    MaterialDialog dialog = new MaterialDialog.Builder(getMainActivity())
                            .title("Перейти к странице")
                            .customView(view, false)
                            .positiveText("Перейти")
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    getContext().openFromSt(listView.getCheckedItemPosition() * postsPerPage);
                                }
                            })
                            .negativeText("Отмена")
                            .cancelable(true)
                            .show();
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                } catch (Throwable ex) {
                    AppLog.e(getMainActivity(), ex);
                }
            }
        });

    }

    @JavascriptInterface
    public void plusRep(final String postId, final String userId, final String userNick) {
        run(new Runnable() {
            @Override
            public void run() {
                getContext().showChangeRep(postId, userId, userNick, "add", "Повысить репутацию");
            }
        });
    }

    @JavascriptInterface
    public void minusRep(final String postId, final String userId, final String userNick) {
        run(new Runnable() {
            @Override
            public void run() {
                getContext().showChangeRep(postId, userId, userNick, "minus", "Понизить репутацию");
            }
        });
    }

    @JavascriptInterface
    public void claim(final String postId) {
        run(new Runnable() {
            @Override
            public void run() {
                org.softeg.slartus.forpdaplus.classes.Post.claim(getMainActivity(), new Handler(), getContext().getTopic().getId(), postId);
            }
        });

    }

    @JavascriptInterface
    public void showRepMenu(final String postId, final String userId, final String userNick, final String canPlus, final String canMinus) {
        run(new Runnable() {
            @Override
            public void run() {
                List<String> items = new ArrayList<>();

                int i = 0;

                int plusRepPosition = -1;
                int showRepPosition = -1;
                int minusRepPosition = -1;
                if ("1".equals(canPlus)) {
                    items.add("Повысить (+1)");
                    plusRepPosition = i; i++;
                }

                items.add("Посмотреть");
                showRepPosition = i; i++;

                if ("1".equals(canMinus)) {
                    items.add("Понизить (-1)");
                    minusRepPosition = i; i++;
                }

                if (items.size() == 0) return;

                final int finalMinusRepPosition = minusRepPosition;
                final int finalShowRepPosition = showRepPosition;
                final int finalPlusRepPosition = plusRepPosition;
                new MaterialDialog.Builder(getMainActivity())
                        .title("Репутация "+userNick)
                        .items(items.toArray(new CharSequence[items.size()]))
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                if (i == finalMinusRepPosition) {
                                    UserReputationFragment.minusRep(getMainActivity(), new Handler(), postId, userId, userNick);
                                } else if (i == finalShowRepPosition) {
                                    getContext().showRep(userId);
                                } else if (i == finalPlusRepPosition) {
                                    UserReputationFragment.plusRep(getMainActivity(), new Handler(), postId, userId, userNick);
                                }
                            }
                        })
                        .show();
            }
        });

    }

    @JavascriptInterface
    public void go_gadget_show() {
        run(new Runnable() {
            @Override
            public void run() {

                String url = "http://4pda.ru/forum/index.php?&showtopic=" + getContext().getTopic().getId() + "&mode=show&poll_open=true&st=" +
                        getContext().getTopic().getCurrentPage() * getContext().getTopic().getPostsPerPageCount(getContext().getLastUrl());
                getContext().showTheme(url);
            }
        });

    }

    @JavascriptInterface
    public void go_gadget_vote() {
        run(new Runnable() {
            @Override
            public void run() {
                String url = "http://4pda.ru/forum/index.php?&showtopic=" + getContext().getTopic().getId() + "&poll_open=true&st=" +
                        getContext().getTopic().getCurrentPage() * getContext().getTopic().getPostsPerPageCount(getContext().getLastUrl());
                getContext().showTheme(url);
            }
        });

    }

    @JavascriptInterface
    public void sendPostsAttaches(final String json) {
        run(new Runnable() {
            @Override
            public void run() {
                for(JsonElement s:new JsonParser().parse(json).getAsJsonArray()){
                    ArrayList<String> list1 = new ArrayList<>();
                    for(JsonElement a:s.getAsJsonArray())
                        list1.add(a.getAsString());
                    getContext().imageAttaches.add(list1);
                }
            }
        });

    }


    public void run(final Runnable runnable) {
        //Почему-то перестало работать как раньше
        /*if (Build.VERSION.SDK_INT < 17) {
            runnable.run();
        } else {
            getMainActivity().runOnUiThread(runnable);
        }*/
        getMainActivity().runOnUiThread(runnable);
    }
}