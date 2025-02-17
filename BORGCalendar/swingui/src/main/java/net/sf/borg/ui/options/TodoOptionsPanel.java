/**
 *Created on Oct 24, 2010
 */
package net.sf.borg.ui.options;

import net.sf.borg.common.PrefName;
import net.sf.borg.common.Resource;
import net.sf.borg.ui.options.OptionsView.OptionsPanel;
import net.sf.borg.ui.util.GridBagConstraintsFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Options for the Todo tab. Controls the behavior of the add quick todo items.
 * 
 * @author thein
 * 
 */
public class TodoOptionsPanel extends OptionsPanel {

	private static final long serialVersionUID = -4357089819869820396L;

	/**
	 * If set, the text field is cleared on creation of a new todo item.
	 */
	private final JCheckBox autoClearTextField = new JCheckBox();

	/**
	 * If set, the date field is defaulted to today's date initially and after
	 * creating a new todo item.
	 */
	private final JCheckBox todoQuickEntryAutoSetDateField = new JCheckBox();
	
	/**
	 * If set, only the current todo is shown, if the todo repeats.
	 */
	private final JCheckBox todoOnlyShowCurrent = new JCheckBox();


	/**
	 * Instantiates a new Todo Options Panel.
	 */
	public TodoOptionsPanel() {
		autoClearTextField.setName("autoClearTextField");
		autoClearTextField.setHorizontalAlignment(SwingConstants.LEFT);
		todoQuickEntryAutoSetDateField.setName("todoQuickEntryAutoSetDateField");
		todoQuickEntryAutoSetDateField.setHorizontalAlignment(SwingConstants.LEFT);
		todoOnlyShowCurrent.setName("todoOnlyShowCurrent");
		todoOnlyShowCurrent.setHorizontalAlignment(SwingConstants.LEFT);

		GridBagConstraints gridBagConstraints = GridBagConstraintsFactory.create(0, -1, GridBagConstraints.NONE);
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		
		this.setLayout(new GridBagLayout());
		this.add(autoClearTextField, gridBagConstraints);
		this.add(todoQuickEntryAutoSetDateField, gridBagConstraints);
		this.add(todoOnlyShowCurrent, gridBagConstraints);
		
		autoClearTextField.setText(Resource.getResourceString("todo_option_auto_clear_text"));
		todoQuickEntryAutoSetDateField.setText(Resource.getResourceString("todo_option_auto_date_today"));
		todoOnlyShowCurrent.setText(Resource.getResourceString("todo_only_show_current"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.borg.ui.options.OptionsView.OptionsPanel#getPanelName()
	 */
	@Override
	public String getPanelName() {
		return Resource.getResourceString("todoOptions");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.borg.ui.options.OptionsView.OptionsPanel#applyChanges()
	 */
	@Override
	public void applyChanges() {
		OptionsPanel.setBooleanPref(autoClearTextField, PrefName.TODO_QUICK_ENTRY_AUTO_CLEAR_TEXT_FIELD);
		OptionsPanel.setBooleanPref(todoQuickEntryAutoSetDateField, PrefName.TODO_QUICK_ENTRY_AUTO_SET_DATE_FIELD);
		OptionsPanel.setBooleanPref(todoOnlyShowCurrent, PrefName.TODO_ONLY_SHOW_CURRENT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.borg.ui.options.OptionsView.OptionsPanel#loadOptions()
	 */
	@Override
	public void loadOptions() {
		OptionsPanel.setCheckBox(autoClearTextField, PrefName.TODO_QUICK_ENTRY_AUTO_CLEAR_TEXT_FIELD);
		OptionsPanel.setCheckBox(todoQuickEntryAutoSetDateField, PrefName.TODO_QUICK_ENTRY_AUTO_SET_DATE_FIELD);
		OptionsPanel.setCheckBox(todoOnlyShowCurrent, PrefName.TODO_ONLY_SHOW_CURRENT);
	}

}
