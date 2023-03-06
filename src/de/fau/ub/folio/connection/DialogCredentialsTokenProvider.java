package de.fau.ub.folio.connection;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**This class shows a Swing dialog window in order to retrieve credentials
 * from user.
 * 
 * @author Martin Scholz, Universitätsbibliothek Erlangen-Nürnberg
 *
 */
public class DialogCredentialsTokenProvider extends CredentialsTokenProvider {

	class CustomDialog extends JDialog implements PropertyChangeListener {
		private static final long serialVersionUID = -825377825371633010L;

		private Credentials cred = null;
		
		private JOptionPane optionPane;
		private JTextField usernameField;
		private JPasswordField passwordField;
				
		private String msg = "Bitte melden Sie sich an.";
		private String labelUsername = "Benutzername:";
		private String labelPassword = "Passwort:";
		private String btnLabelProceed = "Weiter";
		private String btnLabelCancel = "Abbrechen";

		/** Creates the reusable dialog. */
		public CustomDialog(Frame aFrame) {
			super(aFrame);
			setTitle("Anmelden");
			setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
			//Create an array of the text and components to be displayed.
			usernameField = new JTextField();
			passwordField = new JPasswordField();
			Object[] contents = {msg, labelUsername, usernameField, labelPassword, passwordField};

			//Create an array specifying the number of dialog buttons
			//and their text.
			Object[] options = {btnLabelProceed, btnLabelCancel};
	
			//Create the JOptionPane.
			optionPane = new JOptionPane(contents,
					JOptionPane.PLAIN_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION,
					null,
					options,
					options[0]);
			setContentPane(optionPane);
			// Set window options
			setAlwaysOnTop(true);
			setSize(300, 200);
			setLocationByPlatform(true);
			setResizable(false);

			//Handle window closing correctly.
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					/*
					 * Instead of directly closing the window,
					 * we're going to change the JOptionPane's
					 * value property.
					 */
					optionPane.setValue(JOptionPane.CANCEL_OPTION);
				}
			});

			//Ensure the text field always gets the first focus.
			addComponentListener(new ComponentAdapter() {
				public void componentShown(ComponentEvent ce) {
					usernameField.requestFocusInWindow();
				}
			});

			//Register an event handler that reacts to option pane state changes.
			optionPane.addPropertyChangeListener(this);
		}

		/** This method reacts to state changes in the option pane. */
		public void propertyChange(PropertyChangeEvent e) {
			String prop = e.getPropertyName();

			if (isVisible()
					&& (e.getSource() == optionPane)
					&& (JOptionPane.VALUE_PROPERTY.equals(prop) ||
							JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
				Object value = optionPane.getValue();

				if (value == JOptionPane.UNINITIALIZED_VALUE) {
					//ignore reset
					return;
				}

				//Reset the JOptionPane's value.
				//If you don't do this, then if the user
				//presses the same button next time, no
				//property change event will be fired.
				optionPane.setValue(
						JOptionPane.UNINITIALIZED_VALUE);
				
				// clear credentials and only 
				cred = null;
				if (btnLabelProceed.equals(value)) {
					String username = usernameField.getText();
					char[] password = passwordField.getPassword();
					cred = new Credentials() {
						public String username() { return username; }
						public String userId() { return null; }
						public char[] password() { return password; }
						public void erase() { Arrays.fill(password, '0'); }
					};
				}
				clearAndHide();
			}
		}

		/** This method clears the dialog and hides it. */
		public void clearAndHide() {
			usernameField.setText(null);
			passwordField.setText(null);
			setVisible(false);
		}
		
		/** This method clears the dialog and hides it. */
		public void clearCredentials() {
			cred = null;
		}
		
		public Credentials getCredentials() {
			return cred;
		}
		
	}
	
	
	public DialogCredentialsTokenProvider() {
		super();
	}


	@Override
	protected Credentials getCredentials(OkapiConnection okapi) {
		if (!isMethodAvailable()) throw new AuthenticationException("no graphics frontend available");
		CustomDialog dialog = new CustomDialog(null);
		dialog.setVisible(true);
		Credentials cred = dialog.getCredentials();
		dialog.clearCredentials();
		return cred;
	}
	
	
	public static boolean isMethodAvailable() {
		// we need a graphics frontend for displaying a dialog
		return !GraphicsEnvironment.isHeadless();
	}

	
	
	
}
