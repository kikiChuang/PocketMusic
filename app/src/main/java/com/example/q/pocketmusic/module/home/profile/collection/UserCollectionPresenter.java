package com.example.q.pocketmusic.module.home.profile.collection;

import android.content.Intent;
import android.support.annotation.NonNull;

import com.example.q.pocketmusic.callback.ToastQueryListener;
import com.example.q.pocketmusic.callback.ToastUpdateListener;
import com.example.q.pocketmusic.config.Constant;
import com.example.q.pocketmusic.data.bean.Song;
import com.example.q.pocketmusic.data.bean.SongObject;
import com.example.q.pocketmusic.data.bean.collection.CollectionPic;
import com.example.q.pocketmusic.data.bean.collection.CollectionSong;
import com.example.q.pocketmusic.data.model.UserCollectionModel;
import com.example.q.pocketmusic.module.common.BasePresenter;
import com.example.q.pocketmusic.module.common.IBaseView;
import com.example.q.pocketmusic.module.song.SongActivity;
import com.example.q.pocketmusic.util.UserUtil;
import com.example.q.pocketmusic.util.common.ToastUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 鹏君 on 2016/11/14.
 */

public class UserCollectionPresenter extends BasePresenter<UserCollectionPresenter.IView> {
    private IView activity;
    private UserCollectionModel userCollectionModel;
    private int mPage;

    public UserCollectionPresenter(IView activity) {
        attachView(activity);
        this.activity = getIViewRef();
        userCollectionModel = new UserCollectionModel();
        this.mPage = 0;
    }

    //获得收藏曲谱列表
    public void getCollectionList(final boolean isRefreshing) {
        mPage++;
        if (isRefreshing) {
            mPage = 0;//置为零0
        }
        userCollectionModel.getUserCollectionList(UserUtil.user, mPage, new ToastQueryListener<CollectionSong>() {
            @Override
            public void onSuccess(List<CollectionSong> list) {
                activity.setCollectionList(isRefreshing, list);
            }
        });
    }


    //先查询，后进入SongActivity
    public void queryAndEnterSongActivity(final CollectionSong collectionSong) {
        activity.showLoading(true);
        userCollectionModel.getCollectionPicList(collectionSong, new ToastQueryListener<CollectionPic>(activity) {
            @Override
            public void onSuccess(List<CollectionPic> list) {
                activity.showLoading(false);
                Song song = getSong(list);
                enterSongActivity(song);
            }

            //进入SongActivity
            private void enterSongActivity(Song song) {
                Intent intent = new Intent(activity.getCurrentContext(), SongActivity.class);
                SongObject songObject = new SongObject(song, Constant.FROM_COLLECTION, Constant.MENU_DOWNLOAD_SHARE, Constant.NET);
                intent.putExtra(SongActivity.PARAM_SONG_OBJECT_SERIALIZABLE, songObject);
                activity.getCurrentContext().startActivity(intent);
            }

            //CollectionPic--->Song
            @NonNull
            private Song getSong(List<CollectionPic> list) {
                Song song = new Song();
                song.setName(collectionSong.getName());
                song.setContent(collectionSong.getContent());
                List<String> urls = new ArrayList<>();
                for (CollectionPic pic : list) {
                    urls.add(pic.getUrl());
                }
                song.setIvUrl(urls);
                return song;
            }
        });
    }

    //删除收藏
    public void deleteCollection(final CollectionSong collectionSong) {
        userCollectionModel.deleteCollection(collectionSong, new ToastUpdateListener() {
            @Override
            public void onSuccess() {
                ToastUtil.showToast("已删除");
            }
        });
    }


    //更新收藏曲谱名字
    public void updateConnectionName(CollectionSong item, String str) {
        userCollectionModel.updateConnectionName(item, str, new ToastUpdateListener() {
            @Override
            public void onSuccess() {
                activity.setCollectionList(true, null);
            }
        });
    }


    public interface IView extends IBaseView {

        void setCollectionList(boolean isRefreshing, List<CollectionSong> list);
    }
}
