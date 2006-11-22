/*
 * Copyright 2002-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.richclient.samples.simple.ui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.springframework.binding.value.ValueModel;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.richclient.application.PageComponentContext;
import org.springframework.richclient.application.event.LifecycleApplicationEvent;
import org.springframework.richclient.application.support.AbstractView;
import org.springframework.richclient.command.ActionCommandExecutor;
import org.springframework.richclient.command.CommandGroup;
import org.springframework.richclient.command.GuardedActionCommandExecutor;
import org.springframework.richclient.command.support.AbstractActionCommandExecutor;
import org.springframework.richclient.command.support.GlobalCommandIds;
import org.springframework.richclient.dialog.ConfirmationDialog;
import org.springframework.richclient.list.ListSelectionValueModelAdapter;
import org.springframework.richclient.list.ListSingleSelectionGuard;
import org.springframework.richclient.samples.simple.domain.Contact;
import org.springframework.richclient.samples.simple.domain.ContactDataStore;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

/**
 * This class provides the main view of the contacts. It provides a table showing the contact objects and a quick filter
 * field to narrow down the list of visible contacts. Several commands are tied to the selection of the contacts table
 * <p>
 * By implementing special tag interfaces, this component will be automatically wired in to certain events of interest.
 * <ul>
 * <li><b>ApplicationListener</b> - This component will be automatically registered as a listener for application
 * events.</li>
 * </ul>
 * @author Larry Streepy
 */
public class ContactView extends AbstractView implements ApplicationListener {

	/** The object table holding our contacts. */
	private ContactTable contactTable;

	/** The data store holding all our contacts. */
	private ContactDataStore contactDataStore;

	/**
	 * Handler for the "New Contact" action.
	 */
	private ActionCommandExecutor newContactExecutor = new NewContactExecutor();

	/**
	 * Handler for the "Properties" action.
	 */
	private GuardedActionCommandExecutor propertiesExecutor = new PropertiesExecutor();

	/**
	 * Handler for the "Delete" action.
	 */
	private GuardedActionCommandExecutor deleteExecutor = new DeleteExecutor();

	/**
	 * The text field allowing the user to filter the contents of the contact table.
	 */
	private JTextField filterField;

	/**
	 * Default constructor.
	 */
	public ContactView() {
	}

	/**
	 * @return the contactDataStore
	 */
	protected ContactDataStore getContactDataStore() {
		return contactDataStore;
	}

	/**
	 * @param contactDataStore the contactDataStore to set
	 */
	public void setContactDataStore(ContactDataStore contactDataStore) {
		this.contactDataStore = contactDataStore;
	}

	/**
	 * Create the control for this view. This method is called by the platform in order to obtain the control to add to
	 * the surrounding window and page.
	 * @return component holding this view
	 */
	protected JComponent createControl() {
		JPanel filterPanel = new JPanel(new BorderLayout());
		JLabel filterLabel = getComponentFactory().createLabel("nameAddressFilter.label");
		filterPanel.add(filterLabel, BorderLayout.WEST);

		String tip = getMessage("nameAddressFilter.caption");
		filterField = getComponentFactory().createTextField();
		filterField.setToolTipText(tip);
		filterPanel.add(filterField, BorderLayout.CENTER);
		filterPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		contactTable = new ContactTableFactory().createContactTable();

		JPanel view = new JPanel(new BorderLayout());
		JScrollPane sp = getComponentFactory().createScrollPane(contactTable.getControl());
		view.add(filterPanel, BorderLayout.NORTH);
		view.add(sp, BorderLayout.CENTER);
		return view;
	}

	/**
	 * Register the local command executors to be associated with named commands. This is called by the platform prior
	 * to making the view visible.
	 */
	protected void registerLocalCommandExecutors(PageComponentContext context) {
		context.register("newContactCommand", newContactExecutor);
		context.register(GlobalCommandIds.PROPERTIES, propertiesExecutor);
		context.register(GlobalCommandIds.DELETE, deleteExecutor);
	}

	/**
	 * Prepare the table holding all the Contact objects. This table provides pretty much all the functional operations
	 * within this view. Prior to calling this method the {@link #setContactTable(ContactTable)} will have already been
	 * called as part of the context bean creation.
	 */
	private class ContactTableFactory {
		public ContactTable createContactTable() {
			ContactTable contactTable = new ContactTable(contactDataStore);

			// Get the table instance from our factory
			// Make a double click invoke the properties dialog and plugin the
			// context menu
			contactTable.setDoubleClickHandler(propertiesExecutor);

			// Construct and install our filtering list. This filter will allow the user
			// to simply type data into the txtFilter (JTextField). With the configuration
			// setup below, the text entered by the user will be matched against the values
			// in the lastName and address.address1 properties of the contacts in the table.
			// The GlazedLists filtered lists is used to accomplish this.
			EventList baseList = contactTable.getBaseEventList();
			TextFilterator filterator = GlazedLists.textFilterator(new String[] { "lastName", "address.address1" });
			FilterList filterList = new FilterList(baseList, new TextComponentMatcherEditor(filterField, filterator));

			// Install the fully constructed (layered) list into the table
			contactTable.setFinalEventList(filterList);

			// Install the popup menu
			CommandGroup popup = new CommandGroup();
			popup.add(getWindowCommandManager().getActionCommand("deleteCommand"));
			popup.addSeparator();
			popup.add(getWindowCommandManager().getActionCommand("propertiesCommand"));
			contactTable.setPopupCommandGroup(popup);
			
			// Register to get notified when the filtered list changes
			contactTable.setStatusBar(getStatusBar());

			// Ensure our commands are only active when something is selected.
			// These guard objects operate by inspecting a list selection model
			// (held within a ValueModel) and then either enabling or disabling the
			// guarded object (our executors) based on the configured criteria.
			// This configuration greatly simplifies the interaction between commands
			// that require a selection on which to operate.
			ValueModel selectionHolder = new ListSelectionValueModelAdapter(contactTable.getSelectionModel());
			new ListSingleSelectionGuard(selectionHolder, deleteExecutor);
			new ListSingleSelectionGuard(selectionHolder, propertiesExecutor);
			
			return contactTable;
		}
	}

	/**
	 * Private inner class to create a new contact.
	 */
	private class NewContactExecutor implements ActionCommandExecutor {
		public void execute() {
			// We know exactly one contact will be selected at this time because
			// of the guards put in place in prepareTable.
			new ContactPropertiesDialog(getContactDataStore()).showDialog();
		}
	}

	/**
	 * Private inner class to handle the properties form display.
	 */
	private class PropertiesExecutor extends AbstractActionCommandExecutor {
		public void execute() {
			// We know exactly one contact will be selected at this time because
			// of the guards put in place in prepareTable.
			new ContactPropertiesDialog(contactTable.getSelectedContact(), getContactDataStore()).showDialog();
		}
	}

	/**
	 * Private class to handle the delete command. Note that due to the configuration above, this executor is only
	 * enabled when exactly one contact is selected in the table. Thus, we don't have to protect against being executed
	 * with an incorrect state.
	 */
	private class DeleteExecutor extends AbstractActionCommandExecutor {
		public void execute() {
			// We know exactly one contact will be selected at this time because
			// of the guards put in place in prepareTable.
			final Contact contact = contactTable.getSelectedContact();
			// Query the user to be sure they want to do this
			String title = getMessage("contact.confirmDelete.title");
			String message = getMessage("contact.confirmDelete.message");
			ConfirmationDialog dlg = new ConfirmationDialog(title, message) {
				protected void onConfirm() {
					// Delete the object from the persistent store.
					getContactDataStore().delete(contact);
					// And notify the rest of the application of the change
					getApplicationContext().publishEvent(
							new LifecycleApplicationEvent(LifecycleApplicationEvent.DELETED, contact));
				}
			};
			dlg.showDialog();
		}
	}

	/**
	 * Handle an application event. This will notify us of object adds, deletes, and modifications. Forward to our
	 * object table for handling.
	 * @param e event to process
	 */
	public void onApplicationEvent(ApplicationEvent e) {
		contactTable.onApplicationEvent(e);
	}
}