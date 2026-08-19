package gui;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.PlainDocument;
import service.AuthService;

public class SignupFrame extends JFrame implements ActionListener {

    RoundedButton signupbtn;
    OutlineButton cancelbtn;
    JTextField username, email, fullname, address, contactnum;
    JPasswordField pass;
    JCheckBox showpass;
    
    JRadioButton female = new JRadioButton("Female");
    JRadioButton male = new JRadioButton("Male");
    JRadioButton other = new JRadioButton("Other");

    ButtonGroup gender = new ButtonGroup();

    private final AuthService authService;

    public SignupFrame(AuthService authService) {
        this.authService = authService;

        gender.add(female);
        gender.add(male);
        gender.add(other);
        setTitle("Sign Up");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 733);
        setLocationRelativeTo(null);
        setResizable(false);

        //bg
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon img = new ImageIcon(getClass().getResource("/Gui_Images/signup2.png"));
                g.drawImage(img.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        backgroundPanel.setLayout(null);
        setContentPane(backgroundPanel);

        female.setBounds(33, 535, 90, 25);
        female.setOpaque(false);
        female.setForeground(Color.BLACK);
        female.setFocusPainted(false);
        female.setFont(new Font("Fira Code", Font.PLAIN, 15));
        backgroundPanel.add(female);

        male.setBounds(130, 535, 70, 25);
        male.setOpaque(false);
        male.setForeground(Color.BLACK);
        male.setFocusPainted(false);
        male.setFont(new Font("Fira Code", Font.PLAIN, 15));
        backgroundPanel.add(male);

        other.setBounds(210, 535, 80, 25);
        other.setOpaque(false);
        other.setForeground(Color.BLACK);
        other.setFocusPainted(false);
        other.setFont(new Font("Fira Code", Font.PLAIN, 15));
        backgroundPanel.add(other);
        
        signupbtn = new RoundedButton("Sign Up", Color.BLACK, Color.WHITE);
        signupbtn.setBounds(19, 590, 420, 40);
        signupbtn.setFont(new Font("Fira Code", Font.BOLD, 18));
        signupbtn.setHoverColor(Color.DARK_GRAY);
        signupbtn.addActionListener(this);
        backgroundPanel.add(signupbtn);

        cancelbtn = new OutlineButton("Cancel", Color.BLACK, Color.BLACK);
        cancelbtn.setBounds(19, 640, 420, 40);
        cancelbtn.setFont(new Font("Fira Code", Font.BOLD, 18));
        cancelbtn.setBgColor(new Color(0, 0, 0, 60));
        cancelbtn.addActionListener(this);
        backgroundPanel.add(cancelbtn);

        email = new JTextField();
        email.setBounds(33, 129, 390, 30);
        email.setFont(new Font("Fira Code", Font.PLAIN, 14));
        email.setOpaque(false);
        email.setBackground(new Color(0, 0, 0, 0));
        email.setBorder(BorderFactory.createEmptyBorder());
        backgroundPanel.add(email);

        fullname = new JTextField();
        fullname.setBounds(33, 195, 390, 30);
        fullname.setFont(new Font("Fira Code", Font.PLAIN, 14));
        fullname.setOpaque(false);
        fullname.setBackground(new Color(0, 0, 0, 0));
        fullname.setBorder(BorderFactory.createEmptyBorder());
        backgroundPanel.add(fullname);

        username = new JTextField();
        username.setBounds(33, 261, 390, 30);
        username.setFont(new Font("Fira Code", Font.PLAIN, 14));
        username.setOpaque(false);
        username.setBackground(new Color(0, 0, 0, 0));
        username.setBorder(BorderFactory.createEmptyBorder());
        backgroundPanel.add(username);

        pass = new JPasswordField();
        pass.setBounds(33, 327, 390, 30);
        pass.setFont(new Font("Fira Code", Font.PLAIN, 14));
        pass.setOpaque(false);
        pass.setBackground(new Color(0, 0, 0, 0));
        pass.setBorder(BorderFactory.createEmptyBorder());
        backgroundPanel.add(pass);

        showpass = new JCheckBox ("Show Password");
        showpass.setBounds(33, 360, 150, 20);
        showpass.setOpaque(false);
        showpass.setForeground(Color.BLACK);
        showpass.setFocusPainted(false);

        showpass.setFont(new Font("Fira Code", Font.PLAIN, 12));
        showpass.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (showpass.isSelected()) {
                    pass.setEchoChar((char) 0);
                } else {
                    pass.setEchoChar('*');
                }
            }
        });
        backgroundPanel.add(showpass);

        address = new JTextField();
        address.setBounds(33, 415, 390, 30);
        address.setFont(new Font("Fira Code", Font.PLAIN, 14));
        address.setOpaque(false);
        address.setBackground(new Color(0, 0, 0, 0));
        address.setBorder(BorderFactory.createEmptyBorder());
        backgroundPanel.add(address);

        contactnum = new JTextField();
        contactnum.setBounds(33, 486, 390, 30);
        contactnum.setFont(new Font("Fira Code", Font.PLAIN, 14));
        contactnum.setOpaque(false);
        contactnum.setBackground(new Color(0, 0, 0, 0));
        contactnum.setBorder(BorderFactory.createEmptyBorder());
        contactnum.setDocument(new PlainDocument() {
            @Override
            public void insertString(int offs, String str, javax.swing.text.AttributeSet a)
                    throws javax.swing.text.BadLocationException {
                if (str == null)
                    return;
                if (!str.matches("\\d+")) {
                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(null, "Only numbers are allowed!", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    contactnum.setText("");
                    return;
                }
                if (getLength() + str.length() <= 12) {
                    super.insertString(offs, str, a);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(null, "Maximum of 12 numbers only!", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    contactnum.setText("");
                }
            }
        });
        backgroundPanel.add(contactnum);

        

        

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == cancelbtn) {
            dispose();
            new LoginFrame(authService).setVisible(true);
    }

    

    }
}
