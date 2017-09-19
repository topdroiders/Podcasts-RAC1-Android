package cat.xojan.random1.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import javax.inject.Inject;

import cat.xojan.random1.R;
import cat.xojan.random1.injection.HasComponent;
import cat.xojan.random1.injection.component.HomeComponent;
import cat.xojan.random1.ui.adapter.HomePagerAdapter;
import cat.xojan.random1.ui.fragment.BaseFragment;
import cat.xojan.random1.ui.fragment.DownloadsFragment;
import cat.xojan.random1.ui.fragment.HourByHourListFragment;
import cat.xojan.random1.ui.fragment.PodcastListFragment;
import cat.xojan.random1.ui.fragment.ProgramFragment;
import cat.xojan.random1.ui.fragment.SectionFragment;
import cat.xojan.random1.viewmodel.PodcastsViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class HomeActivityJava extends BaseActivity implements HasComponent {

    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 20;

    @Inject PodcastsViewModel mViewModel;

    private HomeComponent mComponent;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    HomePagerAdapter mFragmentAdapter;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0
                && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                && intent.getAction() != null
                && intent.getAction().equals(Intent.ACTION_MAIN)) {
            finish();
            return;
        }

        setContentView(R.layout.activity_home);
        findView();
        initView();
        initInjector();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_export_podcasts:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.
                        WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestWriteExternalStoragePermission();
                } else {
                    exportPodcasts();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE: {
                exportPodcasts();
                break;
            }
        }
    }

    private void requestWriteExternalStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_WRITE_EXTERNAL_STORAGE);
    }

    private void initInjector() {
        /*mComponent = DaggerHomeComponent.builder()
                .appComponent(getApplicationComponent())
                .baseActivityModule(getActivityModule())
                .homeModule(new HomeModule(this))
                .build();
        mComponent.inject(this);*/
    }

    private void findView() {
        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        mTabLayout = (TabLayout) findViewById(R.id.tabLayout);
    }

    private void initView() {
        mFragmentAdapter = new HomePagerAdapter(getSupportFragmentManager(), this);
        mFragmentAdapter.addFragment(new ProgramFragment());
        mFragmentAdapter.addFragment(new DownloadsFragment());

        mViewPager.setAdapter(mFragmentAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    public HomeComponent getComponent() {
        return mComponent;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            Fragment fragment = getFragment(HourByHourListFragment.TAG);
            if (fragment == null) fragment = getFragment(SectionFragment.TAG);

            if (fragment != null && getSupportFragmentManager()
                    .findFragmentByTag(PodcastListFragment.TAG) == null) {
                if (((BaseFragment) fragment).handleOnBackPressed()) {
                    return;
                }
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCompositeDisposable.clear();
    }

    private void exportPodcasts() {
        mCompositeDisposable.add(mViewModel.exportPodcasts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::notifyUser));
    }

    private void notifyUser(Boolean b) {
        Toast.makeText(this, getString(R.string.podcasts_exported), Toast.LENGTH_LONG).show();
    }
}