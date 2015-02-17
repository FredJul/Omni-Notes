package net.fred.taskgame.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import net.fred.taskgame.R;
import net.fred.taskgame.fragment.SettingsFragment;


public class SettingsActivity extends ActionBarActivity {

	private Toolbar toolbar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		initUI();
		getFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();
	}


	void initUI() {
		toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
	}
}
