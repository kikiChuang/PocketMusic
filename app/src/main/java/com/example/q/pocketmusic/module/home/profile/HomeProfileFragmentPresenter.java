package com.example.q.pocketmusic.module.home.profile;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.example.q.pocketmusic.callback.ToastUpdateListener;
import com.example.q.pocketmusic.config.CommonString;
import com.example.q.pocketmusic.config.Constant;
import com.example.q.pocketmusic.module.common.BaseActivity;
import com.example.q.pocketmusic.module.common.BasePresenter;
import com.example.q.pocketmusic.module.common.IBaseView;
import com.example.q.pocketmusic.module.home.profile.collection.UserCollectionActivity;
import com.example.q.pocketmusic.module.home.profile.contribution.CoinRankActivity;
import com.example.q.pocketmusic.module.home.profile.gift.GiftActivity;
import com.example.q.pocketmusic.module.home.profile.interest.UserInterestActivity;
import com.example.q.pocketmusic.module.home.profile.post.UserPostActivity;
import com.example.q.pocketmusic.module.home.profile.setting.SettingActivity;
import com.example.q.pocketmusic.module.home.profile.share.UserShareActivity;
import com.example.q.pocketmusic.module.home.profile.support.SupportActivity;
import com.example.q.pocketmusic.util.LocalPhotoAlbumUtil;
import com.example.q.pocketmusic.util.UserUtil;
import com.example.q.pocketmusic.util.common.IntentUtil;
import com.example.q.pocketmusic.util.common.ToastUtil;

import java.io.File;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.UploadFileListener;

/**
 * Created by 鹏君 on 2017/1/26.
 */

public class HomeProfileFragmentPresenter extends BasePresenter<HomeProfileFragmentPresenter.IView> {
    private IView fragment;


    public HomeProfileFragmentPresenter(IView fragment) {
        attachView(fragment);
        this.fragment = getIViewRef();
    }


    //选择头像
    public void setHeadIv() {

        new LocalPhotoAlbumUtil().getSingleLocalPhoto(fragment, new LocalPhotoAlbumUtil.OnLoadSingleResult() {
            @Override
            public void onSinglePath(final String picPath) {
                //图片上传至Bmob
                final BmobFile bmobFile = new BmobFile(new File(picPath));
                bmobFile.upload(new UploadFileListener() {
                    @Override
                    public void done(BmobException e) {
                        if (e != null) {
                            fragment.showLoading(false);
                            ToastUtil.showToast(CommonString.STR_ERROR_INFO + e.getMessage());
                            return;
                        }
                        //修改用户表的headIv属性
                        UserUtil.user.setHeadImg(bmobFile.getFileUrl());
                        UserUtil.user.update(new ToastUpdateListener(fragment) {
                            @Override
                            public void onSuccess() {
                                fragment.showLoading(false);
                                fragment.setHeadIvResult(picPath);
                            }
                        });

                    }
                });
            }
        });
    }


    //跳转到设置界面
    public void enterSettingActivity() {
        fragment.getCurrentContext().startActivity(new Intent(fragment.getCurrentContext(), SettingActivity.class));
    }

    //调转到收藏界面
    public void enterCollectionActivity() {
        fragment.getCurrentContext().startActivity(new Intent(fragment.getCurrentContext(), UserCollectionActivity.class));
    }


    //签到
    public void addCoin(final int coin) {
        UserUtil.increment(coin, new ToastUpdateListener() {
            @Override
            public void onSuccess() {
                UserUtil.user.setLastSignInDate(dateFormat.format(new Date()));//设置最新签到时间
                UserUtil.user.update(new ToastUpdateListener() {
                    @Override
                    public void onSuccess() {
                        ToastUtil.showToast(CommonString.ADD_COIN_BASE + coin);
                    }
                });
            }
        });
    }

    //检测是否已经签到
    public boolean isSignIn() {
        if (UserUtil.user.getLastSignInDate() == null) {//之前没有这个列,可以签到
            return false;
        } else {
            String lastSignIn = UserUtil.user.getLastSignInDate();
            try {
                Date last = dateFormat.parse(lastSignIn);
                Date now = new Date();
                Calendar cd = Calendar.getInstance();
                cd.setTime(last);
                int lastDay = cd.get(Calendar.DAY_OF_YEAR);
                int lastYear = cd.get(Calendar.YEAR);
                cd.setTime(now);
                int nowDay = cd.get(Calendar.DAY_OF_YEAR);
                int nowYear = cd.get(Calendar.YEAR);
                if (nowYear > lastYear) {
                    return false;//没有签到
                } else {
                    if (nowDay > lastDay) {
                        return false;//没有签到
                    } else {
                        return true;//已经签到
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
                return true;
            }
        }
    }

    //检测是否已经签到
    public void SignIn() {
        if (UserUtil.user.getLastSignInDate() == null) {//之前没有这个列
            final int coin = 5;
            UserUtil.user.setLastSignInDate(dateFormat.format(new Date()));//设置当前时间为最后时间
            UserUtil.increment(coin, new ToastUpdateListener() {
                @Override
                public void onSuccess() {
                    ToastUtil.showToast("今天已签到：" + CommonString.ADD_COIN_BASE + coin);
                }
            });//第一次都加5
        } else {
            fragment.alertSignInDialog();
        }
    }

    //分享apk
    public void shareApp() {
        IntentUtil.shareText(fragment.getCurrentContext(), "推荐一款app:" + "<口袋乐谱>" + "---官网地址：" + "http://pocketmusic.bmob.site/");
    }

    //进入硬币榜
    public void enterContributionActivity() {
        fragment.getCurrentContext().startActivity(new Intent(fragment.getCurrentContext(), CoinRankActivity.class));
    }


    public void enterUserPostActivity() {
        fragment.getCurrentContext().startActivity(new Intent(fragment.getCurrentContext(), UserPostActivity.class));
    }

    //App商店
    public void grade() {
        IntentUtil.enterAppStore(fragment.getCurrentContext());
    }

    public void enterUserShareActivity() {
        fragment.getCurrentContext().startActivity(new Intent(fragment.getCurrentContext(), UserShareActivity.class));
    }

    public void enterSupportActivity() {
        fragment.getCurrentContext().startActivity(new Intent(fragment.getCurrentContext(), SupportActivity.class));
    }

    public void setSignature(final String signature) {
        UserUtil.user.setSignature(signature);
        UserUtil.user.update(new ToastUpdateListener() {
            @Override
            public void onSuccess() {
                ToastUtil.showToast("已修改签名~");
                fragment.setSignature(signature);

            }
        });
    }

    //设置用户属于哪个版本
    public void setUserBelongToVersion() {
        try {
            PackageManager pm = fragment.getCurrentContext().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(fragment.getCurrentContext().getPackageName(), PackageManager.GET_ACTIVITIES);
            UserUtil.user.setVersion(pi.versionName);
            UserUtil.user.update(new ToastUpdateListener() {
                @Override
                public void onSuccess() {

                }
            });
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    //修改昵称
    public void setNickName(final String nickName) {
        boolean isEnough = UserUtil.checkUserContribution((BaseActivity) fragment.getCurrentContext(), Constant.REDUCE_CHANG_NICK_NAME);
        if (isEnough) {
            UserUtil.user.setNickName(nickName);
            UserUtil.user.update(new ToastUpdateListener() {
                @Override
                public void onSuccess() {
                    UserUtil.increment(-Constant.REDUCE_CHANG_NICK_NAME, new ToastUpdateListener() {
                        @Override
                        public void onSuccess() {
                            ToastUtil.showToast(CommonString.REDUCE_COIN_BASE + Constant.REDUCE_CHANG_NICK_NAME);
                            fragment.setNickName(nickName);
                        }
                    });
                }
            });
        } else {
            ToastUtil.showToast(CommonString.STR_NOT_ENOUGH_COIN);
        }
    }


    public void enterGiftActivity() {
        fragment.getCurrentContext().startActivity(new Intent(fragment.getCurrentContext(), GiftActivity.class));
    }

    public void enterInterestActivity() {
        fragment.getCurrentContext().startActivity(new Intent(fragment.getCurrentContext(), UserInterestActivity.class));
    }


    public interface IView extends IBaseView {

        void setHeadIvResult(String photoPath);

        void alertSignInDialog();

        void setSignature(String signature);

        void setNickName(String nickName);
    }
}
