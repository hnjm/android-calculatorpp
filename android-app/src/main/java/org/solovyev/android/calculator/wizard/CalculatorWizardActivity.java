package org.solovyev.android.calculator.wizard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.solovyev.android.calculator.R;

import javax.annotation.Nonnull;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.solovyev.android.calculator.wizard.Wizard.FLOW;
import static org.solovyev.android.calculator.wizard.Wizard.STEP;

/**
 * User: serso
 * Date: 6/16/13
 * Time: 9:07 PM
 */
public final class CalculatorWizardActivity extends SherlockFragmentActivity {

	/*
	**********************************************************************
	*
	*                           FIELDS
	*
	**********************************************************************
	*/
	private WizardStep step;

	private WizardFlow flow;

	/*
	**********************************************************************
	*
	*                           VIEWS
	*
	**********************************************************************
	*/

	private View prevButton;
	private View nextButton;
	private View finishButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.cpp_wizard);

		prevButton = findViewById(R.id.wizard_prev_button);
		nextButton = findViewById(R.id.wizard_next_button);
		finishButton = findViewById(R.id.wizard_finish_button);

		String wizardName = getIntent().getStringExtra(FLOW);
		String stepName = getIntent().getStringExtra(STEP);
		if (savedInstanceState != null) {
			wizardName = savedInstanceState.getString(FLOW);
			stepName = savedInstanceState.getString(STEP);
		}

		flow = Wizard.getWizardFlow(wizardName != null ? wizardName : Wizard.FIRST_TIME_WIZARD);

		WizardStep step = null;
		if(stepName != null) {
			step = flow.getStep(stepName);
		}

		if (step == null) {
			step = flow.getFirstStep();
		}

		setStep(step);
	}

	void setStep(@Nonnull WizardStep step) {
		if (this.step == null || !this.step.equals(step)) {
			hideFragment();
			this.step = step;
			showFragment();

			initTitle();
			initNextButton();
			initFinishButton();
			initPrevButton();
		}
	}

	private void initFinishButton() {
		final WizardStep nextStep = flow.getNextStep(step);
		if (nextStep == null) {
			finishButton.setVisibility(VISIBLE);
			finishButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (tryGoNext()) {
						finishFlow();
					}
				}
			});
		} else {
			finishButton.setVisibility(GONE);
			finishButton.setOnClickListener(null);
		}
	}

	private void initTitle() {
		setTitle(step.getTitleResId());
	}

	private void initPrevButton() {
		final WizardStep prevStep = flow.getPrevStep(step);
		if (prevStep == null) {
			prevButton.setVisibility(GONE);
			prevButton.setOnClickListener(null);
		} else {
			prevButton.setVisibility(VISIBLE);
			prevButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (tryGoPrev()) {
						setStep(prevStep);
					}
				}
			});
		}
	}

	private void initNextButton() {
		final WizardStep nextStep = flow.getNextStep(step);
		if (nextStep == null) {
			nextButton.setVisibility(GONE);
			nextButton.setOnClickListener(null);
		} else {
			nextButton.setVisibility(VISIBLE);
			nextButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (tryGoNext()) {
						setStep(nextStep);
					}
				}
			});
		}
	}

	void finishFlow() {
		finishFlow(false);
	}

	void finishFlow(boolean forceFinish) {
		if (flow != null && step != null) {
			Wizard.saveWizardFinished(flow, step, forceFinish);
		}
		finish();
	}

	private boolean tryGoPrev() {
		if (this.step == null) {
			return true;
		} else {
			final Fragment fragment = getSupportFragmentManager().findFragmentByTag(this.step.getFragmentTag());
			if (fragment != null) {
				return this.step.onPrev(fragment);
			} else {
				return true;
			}
		}
	}

	private boolean tryGoNext() {
		if (this.step == null) {
			return true;
		} else {
			final Fragment fragment = getSupportFragmentManager().findFragmentByTag(this.step.getFragmentTag());
			if (fragment != null) {
				return this.step.onNext(fragment);
			} else {
				return true;
			}
		}
	}

	@Nonnull
	private Fragment showFragment() {
		final FragmentManager fm = getSupportFragmentManager();
		final FragmentTransaction ft = fm.beginTransaction();

		Fragment newFragment = fm.findFragmentByTag(this.step.getFragmentTag());

		if (newFragment == null) {
			newFragment = Fragment.instantiate(this, this.step.getFragmentClass().getName(), this.step.getFragmentArgs());
			ft.add(R.id.wizard_content, newFragment, this.step.getFragmentTag());
		}

		ft.commit();
		fm.executePendingTransactions();

		return newFragment;
	}

	private void hideFragment() {
		final FragmentManager fm = getSupportFragmentManager();
		final FragmentTransaction ft = fm.beginTransaction();

		if (this.step != null) {
			hideFragmentByTag(fm, ft, this.step.getFragmentTag());
		}

		ft.commit();
		fm.executePendingTransactions();
	}

	private void hideFragmentByTag(@Nonnull FragmentManager fm, @Nonnull FragmentTransaction ft, @Nonnull String fragmentTag) {
		final Fragment oldFragment = fm.findFragmentByTag(fragmentTag);
		if (oldFragment != null) {
			ft.remove(oldFragment);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);

		out.putString(FLOW, flow.getName());
		out.putString(STEP, step.getName());
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (flow != null && step != null) {
			Wizard.saveLastWizardStep(flow, step);
		}
	}

	@Override
	public void onBackPressed() {
		FinishWizardConfirmationDialog.show(this);
	}

	WizardStep getStep() {
		return step;
	}

	WizardFlow getFlow() {
		return flow;
	}

	View getPrevButton() {
		return prevButton;
	}

	View getNextButton() {
		return nextButton;
	}

	View getFinishButton() {
		return finishButton;
	}

	/*
	**********************************************************************
	*
	*                           STATIC/INNER
	*
	**********************************************************************
	*/

	public static void startWizard(@Nonnull String name, @Nonnull Context context) {
		final Intent intent = createLaunchIntent(name, context);
		context.startActivity(intent);
	}

	@Nonnull
	private static Intent createLaunchIntent(@Nonnull String name, @Nonnull Context context) {
		final Intent intent = new Intent(context, CalculatorWizardActivity.class);
		intent.putExtra(FLOW, name);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return intent;
	}

	public static void continueWizard(@Nonnull String name, @Nonnull Context context) {
		final String step = Wizard.getLastSavedWizardStepName(name);

		final Intent intent = createLaunchIntent(name, context);
		if (step != null) {
			intent.putExtra(STEP, step);
		}
		context.startActivity(intent);
	}

}
