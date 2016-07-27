package com.aspirephile.shared.parse;

import android.util.Log;

import com.aspirephile.shared.debug.Logger;
import com.aspirephile.shared.debug.NullPointerAsserter;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class CacheSyncManager<T extends ParseObject> {
    private static final int defaultPageSize = 15;
    private final Class<T> subClass;
    ParseImageViewUtils pivUtils;
    List<T> cachedList;
    private String tag;// = CacheSyncManager.class.getName();
    private final DatasetChangeListener defaultDatasetChangeListener = new DatasetChangeListener() {
        @Override
        public void onDatasetChanged() {
            Log.w(tag, "Default dataset change listener has been called");
        }
    };
    private Logger l;
    private final SyncListener<T> defaultSyncListener = new SyncListener<T>() {
        @Override
        public void onLoadingLocalList() {
            l.w("Default local onLoading sync manager callback");
        }

        @Override
        public boolean onLoadedLocalList(List<T> list, ParseException e, List<T> oldList) {
            Log.w(tag, "Default local onLoaded sync manager callback");
            return true;
        }

        @Override
        public void onLoadingOnlineList() {
            Log.w(tag, "Default online onLoading sync manager callback");
        }

        @Override
        public boolean onLoadedOnlineList(List<T> newList, ParseException e, List<T> oldList) {
            Log.w(tag, "Default online onLoaded sync manager callback");
            return true;
        }
    };
    private int pageSize = defaultPageSize;
    private SyncListener<T> syncListener;
    private DatasetChangeListener datasetChangeListener;
    private NullPointerAsserter asserter = new NullPointerAsserter(l);
    private final FindCallback<T> localListLoadedCallback = new FindCallback<T>() {
        @Override
        public void done(List<T> list, ParseException e) {
            if (asserter.assertPointerQuietly(e)) {
                e.printStackTrace();
            } else if (syncListener.onLoadedLocalList(list, e, cachedList)) {
                l.d("Received local list of size: " + list.size());
                setCachedList(list);
            } else {
                l.w("Skipped notification of dataset change as per callback result");
            }
        }
    };
    private ParseQuery<T> localQuery;
    private final FindCallback<T> onlineListLoadedCallback = new FindCallback<T>() {
        @Override
        public void done(List<T> list, ParseException e) {
            if (asserter.assertPointerQuietly(e)) {
                e.printStackTrace();
            } else if (syncListener.onLoadedOnlineList(list, e, cachedList)) {
                l.d("List of size: " + list.size() + " loaded from server.");
                repinListToLocalDatastore(list, cachedList);
            } else {
                l.w("Skipped updating of local datastore as per callback result");
            }
        }
    };
    private ParseQuery<T> onlineQuery;

    public CacheSyncManager(Class<T> subClass, String tag) {
        this.subClass = subClass;
        this.tag = CacheSyncManager.class.getSimpleName() + "_" + tag;
        l = new Logger(tag);
        pivUtils = new ParseImageViewUtils(l);

        syncListener = defaultSyncListener;
        datasetChangeListener = defaultDatasetChangeListener;

        // To prevent null pointer exception if the list is accessed before the callback from local datastore query
        cachedList = new ArrayList<>();
    }

    public static <T extends ParseObject> boolean isListIDsEquivalent(List<T> cachedList, List<T> list) {
        if (cachedList == list)
            return true;
        if (cachedList.size() == list.size()) {
            for (int i = 0; i < cachedList.size(); i++) {
                if (!cachedList.get(i).getObjectId().equals(list.get(i).getObjectId())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void refreshListFromLocalDatastore() {
        l.d("Refreshing list from local datastore");
        syncListener.onLoadingLocalList();
        getLocalListInBackground(localListLoadedCallback);
    }

    public void updateLocalDatastore() {
        l.d("Updating local datastore");
        syncListener.onLoadingOnlineList();
        fetchListFromServer(onlineListLoadedCallback);
    }

    private void fetchListFromServer(FindCallback<T> callback) {
        onlineQuery.findInBackground(callback);
    }

    private void repinListToLocalDatastore(final List<T> list, final List<T> oldList) {
        l.d("Unpinning local list from local datastore");
        T.unpinAllInBackground(tag, new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                if (asserter.assertPointerQuietly(e)) {
                    e.printStackTrace();
                } else {
                    l.d("Successfully unpinned all items in background");
                    if (list.size() > 0) {
                        for (T t : list) {
                            ParseACL publicACL = new ParseACL();
                            publicACL.setPublicReadAccess(true);
                            publicACL.setPublicWriteAccess(true);
                            t.setACL(publicACL);
                        }
                        l.d("Pinning list of size: " + list.size() + " in background");
                        T.pinAllInBackground(tag, list, new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (asserter.assertPointerQuietly(e)) {
                                    e.printStackTrace();
                                } else {
                                    l.d("Successfully pinned list of size: " + list.size());
                                    refreshListFromLocalDatastore();
                                }
                            }
                        });
                    } else {
                        l.w("Cannot pin list of size: " + list.size());
                        refreshListFromLocalDatastore();
                    }
                }
            }
        });
    }

    public void getLocalListInBackground(FindCallback<T> callback) {
        localQuery.findInBackground(callback);
    }

    public void setSyncListener(SyncListener<T> syncListener) {
        if (asserter.assertPointer(syncListener))
            this.syncListener = syncListener;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getCachedList() {
        return cachedList;
    }

    public void setCachedList(List<T> cachedList) {
        if (asserter.assertPointer(cachedList)) {
            l.d("Setting list of size: " + cachedList.size());
            this.cachedList = cachedList;
            datasetChangeListener.onDatasetChanged();
        }
    }

    public void setOnDatasetChangedListener(DatasetChangeListener onDatasetChangedListener) {
        this.datasetChangeListener = onDatasetChangedListener;
    }

    public void setLocalQuery(ParseQuery<T> localQuery) {
        this.localQuery = localQuery;
    }

    public void setOnlineQuery(ParseQuery<T> onlineQuery) {
        this.onlineQuery = onlineQuery;
    }

    public String getPinTag() {
        return tag;
    }

    public interface SyncListener<T extends ParseObject> {

        void onLoadingLocalList();

        boolean onLoadedLocalList(List<T> list, ParseException e, List<T> oldList);

        void onLoadingOnlineList();

        boolean onLoadedOnlineList(List<T> newList, ParseException e, List<T> oldList);
    }

    public interface DatasetChangeListener {
        void onDatasetChanged();
    }
}