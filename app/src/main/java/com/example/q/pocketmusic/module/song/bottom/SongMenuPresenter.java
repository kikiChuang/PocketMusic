package com.example.q.pocketmusic.module.song.bottom;

import android.content.Intent;
import android.support.annotation.NonNull;

import com.example.q.pocketmusic.callback.ToastQueryListListener;
import com.example.q.pocketmusic.callback.ToastQueryListener;
import com.example.q.pocketmusic.callback.ToastSaveListener;
import com.example.q.pocketmusic.callback.ToastUpdateListener;
import com.example.q.pocketmusic.config.BmobConstant;
import com.example.q.pocketmusic.config.CommonString;
import com.example.q.pocketmusic.config.Constant;
import com.example.q.pocketmusic.model.bean.DownloadInfo;
import com.example.q.pocketmusic.model.bean.MyUser;
import com.example.q.pocketmusic.model.bean.Song;
import com.example.q.pocketmusic.model.bean.SongObject;
import com.example.q.pocketmusic.model.bean.ask.AskSongComment;
import com.example.q.pocketmusic.model.bean.collection.CollectionPic;
import com.example.q.pocketmusic.model.bean.collection.CollectionSong;
import com.example.q.pocketmusic.model.bean.local.LocalSong;
import com.example.q.pocketmusic.model.bean.share.ShareSong;
import com.example.q.pocketmusic.model.db.LocalSongDao;
import com.example.q.pocketmusic.module.common.BaseActivity;
import com.example.q.pocketmusic.module.common.BasePresenter;
import com.example.q.pocketmusic.module.common.IBaseView;
import com.example.q.pocketmusic.module.song.SongActivity;
import com.example.q.pocketmusic.util.CheckUserUtil;
import com.example.q.pocketmusic.util.DownloadUtil;
import com.example.q.pocketmusic.util.common.IntentUtil;
import com.example.q.pocketmusic.util.common.LogUtils;
import com.example.q.pocketmusic.util.common.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobBatch;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BatchResult;
import cn.bmob.v3.datatype.BmobPointer;
import cn.bmob.v3.datatype.BmobRelation;

/**
 * Created by 鹏君 on 2017/5/31.
 */

public class SongMenuPresenter extends BasePresenter<SongMenuPresenter.IView> {
    private IView fragment;
    private Intent intent;
    private boolean isEnableAgree = true;//是否能够点赞
    private Song song;
    private int isFrom;

    public SongMenuPresenter(IView fragment) {
        attachView(fragment);
        this.fragment = getIViewRef();
    }


    public Song getSong() {
        return song;
    }


    //下载
    public void download(final String name) {
        DownloadUtil downloadUtil = new DownloadUtil(fragment.getCurrentContext());
        downloadUtil.setOnDownloadListener(new DownloadUtil.OnDownloadListener() {
                                               @Override
                                               public DownloadInfo onStart() {
                                                   fragment.dismissEditDialog();
                                                   return downloadStartCheck();
                                               }

                                               @Override
                                               public void onSuccess() {
                                                   if (isFrom == Constant.FROM_SHARE) {
                                                       addDownLoadNum();//增加下载量
                                                   }
                                                   fragment.downloadResult(Constant.SUCCESS, "下载成功");
                                               }

                                               @Override
                                               public void onFailed(String info) {
                                                   fragment.downloadResult(Constant.FAIL, info);
                                               }
                                           }
        ).downloadBatchPic(name, song.getIvUrl(), song.getTypeId());
    }

    //添加下载量
    private void addDownLoadNum() {
        ShareSong shareSong = (ShareSong) intent.getSerializableExtra(SongActivity.SHARE_SONG);
        shareSong.increment(BmobConstant.BMOB_DOWNLOAD_NUM);
        shareSong.update(new ToastUpdateListener() {
            @Override
            public void onSuccess() {
                LogUtils.e("下载量+1");
            }
        });
    }

    //下载检测
    private DownloadInfo downloadStartCheck() {
        //如果无图
        if (song.getIvUrl() == null || song.getIvUrl().size() <= 0) {
            return new DownloadInfo("没有图片", false);
        }
        //如果本地已经存在
        if (new LocalSongDao(fragment.getCurrentContext()).isExist(song.getName())) {
            return new DownloadInfo("本地已存在", false);
        }
        MyUser user = CheckUserUtil.checkLocalUser((BaseActivity) fragment.getCurrentContext());
        //找不到用户
        if (user == null) {
            return new DownloadInfo("找不到用户", false);
        }
        //硬币不足
        if (!CheckUserUtil.checkUserContribution(((BaseActivity) fragment.getCurrentContext()), Constant.REDUCE_DOWNLOAD)) {
            return new DownloadInfo(CommonString.STR_NOT_ENOUGH_COIN, false);
        }
        //扣除硬币
        user.increment(BmobConstant.BMOB_COIN, -Constant.REDUCE_DOWNLOAD);
        user.update(new ToastUpdateListener(fragment) {
            @Override
            public void onSuccess() {
                ToastUtil.showToast(CommonString.REDUCE_COIN_BASE + (Constant.REDUCE_DOWNLOAD));
            }
        });
        return new DownloadInfo("", true);
    }

    //判断当前的评论的图片是否可以点赞
    public void checkAskHasAgree() {
        BmobQuery<MyUser> query = new BmobQuery<>();
        final MyUser user = MyUser.getCurrentUser(MyUser.class);
        AskSongComment askSongComment = (AskSongComment) intent.getSerializableExtra(SongActivity.ASK_COMMENT);
        query.addWhereRelatedTo(BmobConstant.BMOB_AGREES, new BmobPointer(askSongComment));
        query.findObjects(new ToastQueryListener<MyUser>() {
            @Override
            public void onSuccess(List<MyUser> list) {
                for (MyUser other : list) {
                    if (other.getObjectId().equals(user.getObjectId())) {
                        //已经点赞
                        isEnableAgree = false;
                        break;
                    }
                    isEnableAgree = true;
                }
            }
        });
    }

    //检查分享是否能点赞
    private void checkShareHasAgree() {
        BmobQuery<MyUser> query = new BmobQuery<>();
        final MyUser user = MyUser.getCurrentUser(MyUser.class);
        ShareSong shareSong = (ShareSong) intent.getSerializableExtra(SongActivity.SHARE_SONG);
        query.addWhereRelatedTo("agrees", new BmobPointer(shareSong));
        query.findObjects(new ToastQueryListener<MyUser>() {
            @Override
            public void onSuccess(List<MyUser> list) {
                for (MyUser other : list) {
                    if (other.getObjectId().equals(user.getObjectId())) {
                        //已经点赞
                        isEnableAgree = false;
                        break;
                    }
                    isEnableAgree = true;
                }
            }
        });
    }

    public boolean isEnableAgree() {
        return isEnableAgree;
    }


    //点赞
    public void agree() {
        if (isEnableAgree()) {
            if (isFrom == Constant.FROM_ASK) {
                agreeWithCommentPic();
            }
            if (isFrom == Constant.FROM_SHARE) {
                agreeWithSharePic();
            }

        } else {
            ToastUtil.showToast("已经赞过了哦~");
        }
    }

    //分享点赞
    private void agreeWithSharePic() {
        BmobRelation relation = new BmobRelation();
        final MyUser user = MyUser.getCurrentUser(MyUser.class);
        relation.add(user);
        ShareSong shareSong = (ShareSong) intent.getSerializableExtra(SongActivity.SHARE_SONG);
        shareSong.setAgrees(relation);
        shareSong.increment(BmobConstant.BMOB_AGREE_NUM);//原子操作，点赞数加一
        shareSong.update(new ToastUpdateListener() {
            @Override
            public void onSuccess() {
                ToastUtil.showToast("已点赞");
                user.increment(BmobConstant.BMOB_COIN, Constant.ADD_CONTRIBUTION_AGREE);
                user.update(new ToastUpdateListener() {
                    @Override
                    public void onSuccess() {
                        ToastUtil.showToast(CommonString.ADD_COIN_BASE + Constant.ADD_CONTRIBUTION_AGREE);
                    }
                });
            }
        });
    }

    //评论图片点赞
    private void agreeWithCommentPic() {
        BmobRelation relation = new BmobRelation();
        final MyUser user = MyUser.getCurrentUser(MyUser.class);
        relation.add(user);
        AskSongComment askSongComment = (AskSongComment) intent.getSerializableExtra(SongActivity.ASK_COMMENT);
        askSongComment.setAgrees(relation);
        askSongComment.increment(BmobConstant.BMOB_AGREE_NUM);//原子操作，点赞数加一
        askSongComment.update(new ToastUpdateListener() {
            @Override
            public void onSuccess() {
                ToastUtil.showToast("已点赞");
                user.increment(BmobConstant.BMOB_COIN, Constant.ADD_CONTRIBUTION_AGREE);
                user.update(new ToastUpdateListener() {
                    @Override
                    public void onSuccess() {
                        ToastUtil.showToast(CommonString.ADD_COIN_BASE + Constant.ADD_CONTRIBUTION_AGREE);
                    }
                });
            }
        });
    }


    //添加收藏
    public void addCollection() {
        final MyUser user = CheckUserUtil.checkLocalUser((BaseActivity) fragment.getCurrentContext());
        if (user == null) {
            ToastUtil.showToast("请先登录~");
            return;
        }
        if (song == null) {
            ToastUtil.showToast("发生未知错误，请重新打开乐谱后添加");
            return;
        }

        if (song.getIvUrl() == null || song.getIvUrl().size() <= 0) {
            ToastUtil.showToast("图片为空");
            return;
        }
        //检测是否已经收藏
        BmobQuery<CollectionSong> query = new BmobQuery<>();
        query.order("-updatedAt");
        query.addWhereRelatedTo(BmobConstant.BMOB_COLLECTIONS, new BmobPointer(user));//在user表的Collections找user
        query.findObjects(new ToastQueryListener<CollectionSong>() {
            @Override
            public void onSuccess(List<CollectionSong> list) {
                if (song == null || intent == null) {
                    ToastUtil.showToast("发生未知错误，请重新打开乐谱后添加");
                    return;
                }

                //是否已收藏
                for (CollectionSong collectionSong : list) {
                    if (collectionSong.getName().equals(song.getName())) {
                        ToastUtil.showToast("已收藏");
                        return;
                    }
                }
                //贡献度是否足够
                if (!CheckUserUtil.checkUserContribution(((BaseActivity) fragment.getCurrentContext()), Constant.REDUCE_CONTRIBUTION_COLLECTION)) {
                    ToastUtil.showToast("贡献值不够~");
                    return;
                }
                //添加收藏记录
                final CollectionSong collectionSong = new CollectionSong();
                collectionSong.setName(song.getName());
                collectionSong.setIsFrom(((SongObject) intent.getSerializableExtra(SongActivity.PARAM_SONG_OBJECT_SERIALIZEABLE)).getFrom());
                collectionSong.setContent(song.getContent());
                collectionSong.save(new ToastSaveListener<String>() {

                    @Override
                    public void onSuccess(String s) {
                        if (song == null) {
                            ToastUtil.showToast("发生未知错误，请重新打开乐谱后添加");
                            return;
                        }

                        final int numPic = song.getIvUrl().size();
                        List<BmobObject> collectionPics = new ArrayList<>();
                        for (int i = 0; i < numPic; i++) {
                            CollectionPic collectionPic = new CollectionPic();
                            collectionPic.setCollectionSong(collectionSong);
                            collectionPic.setUrl(song.getIvUrl().get(i));
                            collectionPics.add(collectionPic);
                        }
                        //批量修改
                        new BmobBatch().insertBatch(collectionPics).doBatch(new ToastQueryListListener<BatchResult>() {
                            @Override
                            public void onSuccess(List<BatchResult> list) {
                                BmobRelation relation = new BmobRelation();
                                relation.add(collectionSong);
                                user.setCollections(relation);//添加用户收藏
                                user.update(new ToastUpdateListener() {
                                    @Override
                                    public void onSuccess() {
                                        ToastUtil.showToast("已收藏");
                                        user.increment(BmobConstant.BMOB_COIN, -Constant.REDUCE_CONTRIBUTION_COLLECTION);//贡献值-1
                                        user.update(new ToastUpdateListener() {
                                            @Override
                                            public void onSuccess() {
                                                ToastUtil.showToast(CommonString.REDUCE_COIN_BASE + Constant.REDUCE_CONTRIBUTION_COLLECTION);
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

    }

    //分享乐谱,本地和网络
    public void share() {
        List<String> list = null;
        SongObject songObject = (SongObject) intent.getSerializableExtra(SongActivity.PARAM_SONG_OBJECT_SERIALIZEABLE);
        int loadingWay = songObject.getLoadingWay();
        switch (loadingWay) {
            case Constant.NET:
                list = song.getIvUrl();
                break;
            case Constant.LOCAL:
                list = getLocalImgs();
                break;
        }
        if (list == null || list.size() <= 0) {
            ToastUtil.showToast("没有图片");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String url : list) {
            sb.append(url).append(",");
        }
        //分享
        IntentUtil.shareText(fragment.getCurrentContext(), "推荐一首歌：" + "<<" + song.getName() + ">>:" + sb.toString());
    }

    //得到本地图片
    @NonNull
    private List<String> getLocalImgs() {
        LocalSong localsong = (LocalSong) intent.getSerializableExtra(SongActivity.LOCAL_SONG);
        LocalSongDao localSongDao = new LocalSongDao(fragment.getCurrentContext());
        return localSongDao.getLocalImgsPath(fragment.getCurrentContext(), localsong);
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public void init() {
        SongObject songObject = (SongObject) intent.getSerializableExtra(SongActivity.PARAM_SONG_OBJECT_SERIALIZEABLE);
        isFrom = songObject.getFrom();
        song = songObject.getSong();

        //求谱，检测是否可以点赞,
        if (isFrom == Constant.FROM_ASK) {
            checkAskHasAgree();
        }

        if (isFrom == Constant.FROM_SHARE) {
            checkShareHasAgree();
        }

    }


    public int getShowMenuFlag() {
        return ((SongObject) intent.getSerializableExtra(SongActivity.PARAM_SONG_OBJECT_SERIALIZEABLE)).getShowMenu();
    }


    interface IView extends IBaseView {

        void dismissEditDialog();

        void downloadResult(Integer success, String str);
    }
}
