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
    private int pageSize = defaultPageSize;

    private final String TAG = CacheSyncManager.class.getName();
    private final SyncListener<T> defaultSyncListener = new SyncListener<T>() {
        @Override
        public void onLoadingLocalList() {
            Log.d(TAG, "Default local onLoading organization adapter callback");
        }

        @Override
        public boolean onLoadedLocalList(List<T> list, ParseException e, List<T> oldList) {
            Log.d(TAG, "Default local onLoaded organization adapter callback");
            return true;
        }

        @Override
        public void onLoadingOnlineList() {
            Log.d(TAG, "Default online onLoading organization adapter callback");
        }

        @Override
        public boolean onLoadedOnlineList(List<T> newList, ParseException e, List<T> oldList) {
            Log.d(TAG, "Default online onLoaded organization adapter callback");
            return true;
        }
    };
    private final DatasetChangeListener defaultDatasetChangeListener = new DatasetChangeListener() {
        @Override
        public void onDatasetChanged() {
            Log.w(TAG, "Default dataset change listener has been called");
        }
    };

    private SyncListener<T> syncListener;
    private DatasetChangeListener datasetChangeListener;
    private final Class<T> subClass;
    Logger l = new Logger(CacheSyncManager.class);
    ParseImageViewUtils pivUtils = new ParseImageViewUtils(l);
    List<T> cachedList;
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
    private final FindCallback<T> onlineListLoadedCallback = new FindCallback<T>() {
        @Override
        public void done(List<T> list, ParseException e) {
            if (asserter.assertPointerQuietly(e)) {
                e.printStackTrace();
            } else if (syncListener.onLoadedOnlineList(list, e, cachedList)) {
                l.d("List of size: " + list.size() + " loaded from server.");
                repinListToLocalDatastore(list);
            } else {
                l.w("Skipping updation of local datastore as per callback result");
            }
        }
    };

    public CacheSyncManager(Class<T> subClass) {
        this.subClass = subClass;

        syncListener = defaultSyncListener;
        datasetChangeListener = defaultDatasetChangeListener;

        // To prevent null pointer exception if the list is accessed before the callback from local datastore query
        cachedList = new ArrayList<>();
        refreshListFromLocalDatastore();
        updateLocalDatastore();
    }

    public void refreshListFromLocalDatastore() {
        l.d("Refreshing list from local datastore");
        syncListener.onLoadingLocalList();
        getLocalOrganizationListInBackground(localListLoadedCallback);
    }

    public void updateLocalDatastore() {
        l.d("Updating local organization datastore");
        syncListener.onLoadingOnlineList();
        fetchListFromServer(onlineListLoadedCallback);
    }

    private void fetchListFromServer(FindCallback<T> callback) {
        ParseQuery<T> query = ParseQuery.getQuery(subClass);
        // TODO Fetch only organizations that the user belongs to
        // TODO Select only required keys
        //organizationQuery.include(Organization.fields....);
        query.findInBackground(callback);
    }

    private void repinListToLocalDatastore(final List<T> list) {
        l.d("Pinning list of size: " + list.size() + " in background");
        T.unpinAllInBackground(new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                if (list.size() > 0) {
                    for (T organization : list) {
                        ParseACL publicACL = new ParseACL();
                        publicACL.setPublicReadAccess(true);
                        publicACL.setPublicWriteAccess(true);
                        organization.setACL(publicACL);
                    }
                    T.pinAllInBackground(list, new SaveCallback() {
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
                }
            }
        });
    }

    public void getLocalOrganizationListInBackground(FindCallback<T> callback) {
        ParseQuery<T> localQuery = ParseQuery.getQuery(subClass);
        localQuery
                .setLimit(pageSize)
                .fromLocalDatastore()
                .findInBackground(callback);
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
            l.d("Setting organization list of size: " + cachedList.size());
            this.cachedList = cachedList;
            datasetChangeListener.onDatasetChanged();
        }
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

    public void setOnDatasetChangedListener(DatasetChangeListener onDatasetChangedListener) {
        this.datasetChangeListener = onDatasetChangedListener;
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