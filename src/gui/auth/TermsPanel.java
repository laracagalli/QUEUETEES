package gui.auth;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import gui.components.*;
import model.User;
import service.AuthService;

/** A readable rules page with explicit, unchecked-by-default acceptance. */
public final class TermsPanel extends JPanel {
    public static final String RULES = """
            QUEUETEES — Customer Terms & Conditions

            Please read these rules before using your account.

            1. Account information and security
            Use accurate account and contact information. Keep your password private and use only your own account. Do not try to access another person's account.

            2. Orders and queue fairness
            Place orders you genuinely intend to purchase. Do not create fake or repeated orders to disrupt the queue, reserve stock unfairly, or interfere with other users. Review your details before confirming an order.

            3. Pickup, delivery, cancellations and returns
            Follow the pickup or delivery instructions shown for your order. Keep your contact and delivery details accurate. Give truthful reasons and evidence for cancellations, returns, refunds and delivery issues. Request returns within 30 days of recorded delivery. Report problems through the system's available tools.

            4. Administrator review and warnings
            The system may flag activity for administrator review. Administrators can also report observed issues. A flag is an observation, not automatic proof of misconduct. After review, an administrator may send an account warning or restrict access.

            5. Suspension of access
            Confirmed or repeated misuse may result in temporary or permanent suspension. Permanent suspension blocks sign-in and cannot be reversed using the regular Unsuspend action. A warning or suspension notice will be shown in the system.

            6. Questions and disputed decisions
            Contact the administrator if you do not understand a rule, believe a warning or suspension is mistaken, or need help with your account.

            By checking the box below, you confirm that you have read and agree to follow these rules.
            """;
    public static final String STAFF_RULES = """
            QUEUETEES — Staff Terms & Conditions

            Please read these responsibilities before continuing your staff application.

            1. Staff account and approval
            Provide accurate application details. Verify your email and wait for administrator approval before using the staff dashboard. Creating an account does not grant staff access.

            2. Passwords and assigned permissions
            Keep your password private and use only your assigned account and permissions. Do not share staff access or use another person's account. Log out when you finish using the system.

            3. Customer information
            Use customer names, contact details, addresses and order information only to carry out your assigned work. Do not share customer information with unauthorized people.

            4. Accurate order and delivery updates
            Follow the system's order queue and fulfillment process. Record preparation, transfer, dispatch and delivery updates accurately. Do not confirm a stage that has not happened or enter false delivery references.

            5. Stock, returns and refunds
            Record permitted stock actions accurately. Review return evidence fairly and record actual findings. Do not alter records for personal benefit or claim that a refund was paid without evidence. Refund payment recording remains an administrator responsibility.

            6. Activity review and account access
            Staff activity may be recorded and reviewed by an administrator. Report mistakes or suspicious activity promptly. Confirmed or repeated misuse may lead to temporary or permanent suspension. A flag alone is not proof of misconduct.

            7. Questions and disputed decisions
            Contact the administrator if you need help with your duties or believe a warning or suspension is mistaken.

            By checking the box below, you confirm that you have read and agree to follow these staff responsibilities.
            """;
    private final JCheckBox agreement=new JCheckBox("I have read and agree to the Terms & Conditions.");
    private final JButton continueButton=ActionButtons.create("Agree & Continue",ActionButtons.GREEN);
    private final JLabel feedback=new JLabel("Check the agreement box to continue.");

    public TermsPanel(AuthService auth,User user,Runnable next,Runnable cancel) {
        super(new BorderLayout(0,16));setBackground(AppTheme.PAPER);setBorder(new EmptyBorder(24,30,24,30));
        JPanel heading=new JPanel(new GridLayout(2,1,0,6));heading.setOpaque(false);
        boolean staff=user.getRole()==model.UserRole.STAFF;
        String role=staff?"Staff":"Customer";
        JLabel title=new JLabel(role+" Terms & Conditions");title.setFont(new Font("Segoe UI",Font.BOLD,26));heading.add(title);
        heading.add(new JLabel("Read the rules before continuing • Version "+AuthService.TERMS_VERSION));add(heading,BorderLayout.NORTH);
        JTextArea rules=new JTextArea(staff?STAFF_RULES:RULES);rules.setEditable(false);rules.setLineWrap(true);rules.setWrapStyleWord(true);
        rules.setFont(new Font("Segoe UI",Font.PLAIN,15));rules.setBackground(Color.WHITE);rules.setBorder(new EmptyBorder(18,20,18,20));rules.setCaretPosition(0);
        rules.getAccessibleContext().setAccessibleName("QueueTees "+role.toLowerCase(java.util.Locale.ROOT)+" terms and account review policy");add(new ModernScrollPane(rules));
        agreement.setText("I have read and agree to the "+role+" Terms & Conditions.");
        agreement.setOpaque(false);agreement.setFont(new Font("Segoe UI",Font.BOLD,14));agreement.setSelected(false);
        continueButton.setEnabled(false);
        agreement.addItemListener(e->{continueButton.setEnabled(agreement.isSelected());feedback.setText(agreement.isSelected()?"You can now agree and continue.":"Check the agreement box to continue.");});
        continueButton.addActionListener(e->{
            if(!agreement.isSelected())return;
            continueButton.setEnabled(false);
            try{auth.acceptTerms(user,agreement.isSelected());next.run();}
            catch(RuntimeException ex){feedback.setText("Unable to save your agreement. Please try again.");continueButton.setEnabled(agreement.isSelected());
                JOptionPane.showMessageDialog(this,ex.getMessage(),"Unable to continue",JOptionPane.WARNING_MESSAGE);}
        });
        JButton back=ActionButtons.create("Back to signup",ActionButtons.NEUTRAL);back.addActionListener(e->cancel.run());
        JPanel controls=new JPanel(new gui.components.WrappingFlowLayout());controls.setOpaque(false);controls.add(back);controls.add(continueButton);
        JPanel lower=new JPanel(new BorderLayout(0,10));lower.setOpaque(false);lower.add(agreement,BorderLayout.NORTH);lower.add(feedback);lower.add(controls,BorderLayout.SOUTH);add(lower,BorderLayout.SOUTH);
    }
}
