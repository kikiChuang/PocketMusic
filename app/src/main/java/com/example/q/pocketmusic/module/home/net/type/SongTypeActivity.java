package com.example.q.pocketmusic.module.home.net.type;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.q.pocketmusic.R;
import com.example.q.pocketmusic.model.bean.local.LocalSong;
import com.example.q.pocketmusic.module.common.BaseActivity;
import com.example.q.pocketmusic.util.InstrumentFlagUtil;
import com.example.q.pocketmusic.view.widget.view.MorePopupWindow;
import com.example.q.pocketmusic.view.widget.view.TopTabView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SongTypeActivity extends BaseActivity<SongTypeActivityPresenter.IView, SongTypeActivityPresenter>
        implements SongTypeActivityPresenter.IView, TopTabView.TopTabListener {
    //500*300
    public Integer typeId;
    public final static String PARAM_POSITION = "position";
    @BindView(R.id.type_tab_view)
    TopTabView typeTabView;
    @BindView(R.id.study_iv)
    ImageView studyIv;
    @BindView(R.id.share_iv)
    ImageView shareIv;
    @BindView(R.id.ask_iv)
    ImageView askIv;
    @BindView(R.id.top_iv)
    ImageView topIv;
    @BindView(R.id.type_content)
    FrameLayout typeContent;


    @Override
    public int setContentResource() {
        return R.layout.activity_type_song;
    }

    @Override
    public void initView() {
        //初始化
        //获取乐器类型
        typeId = getIntent().getIntExtra(PARAM_POSITION, 0);
        //设置顶部图片
        topIv.setBackgroundResource(InstrumentFlagUtil.getTopDrawableResource(typeId));

        //初始化Fragemnt
        presenter.setFragmentManager(getSupportFragmentManager());
        presenter.initFragment(typeId);

        //top_view
        typeTabView.setListener(this);
        typeTabView.setCheck(0);
        presenter.clickHotList();
    }


    @Override
    protected SongTypeActivityPresenter createPresenter() {
        return new SongTypeActivityPresenter(this);
    }



    @Override
    public void onSelectHotList() {
        typeTabView.setCheck(0);
    }

    @Override
    public void onSelectCommunity() {
        typeTabView.setCheck(1);
    }

    @Override
    public void alertLocalListDialog(final List<LocalSong> localSongs, List<String> strings) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getCurrentContext()
                , android.R.layout.select_dialog_item
                , android.R.id.text1
                , strings);

        new AlertDialog.Builder(getCurrentContext())
                .setTitle("本地乐谱")
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.enterShareActivity(localSongs.get(which), typeId);
                    }
                })
                .show();
    }


    @Override
    public void setTopTabCheck(int position) {
        if (position == 0) {
            presenter.clickHotList();
        } else {
            presenter.clickCommunity();
        }
    }

    @OnClick({R.id.study_iv, R.id.share_iv, R.id.ask_iv})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.study_iv:
                presenter.enterStudyActivity(typeId);
                break;
            case R.id.share_iv:
                presenter.queryLocalSongList();
                break;
            case R.id.ask_iv:
                presenter.enterPublishAskActivity(typeId);
                break;
        }
    }
}
