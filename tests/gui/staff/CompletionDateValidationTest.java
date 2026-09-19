package gui.staff;
import javax.swing.*;
import java.time.*;
import java.util.Date;
public class CompletionDateValidationTest {
 public static void main(String[] args)throws Exception {SwingUtilities.invokeAndWait(()->{
  CompletedOrdersPanel.PastDateModel model=new CompletedOrdersPanel.PastDateModel();
  JSpinner spinner=new JSpinner(model);JSpinner.DateEditor editor=new JSpinner.DateEditor(spinner,"yyyy-MM-dd");
  editor.getFormat().setLenient(false);spinner.setEditor(editor);
  if(model.getNextValue()!=null)throw new AssertionError("Future day available through arrow");
  Date future=Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
  try{model.setValue(future);throw new AssertionError("Future model value accepted");}catch(IllegalArgumentException expected){}
  editor.getTextField().setText(LocalDate.now().plusDays(1).toString());
  try{spinner.commitEdit();throw new AssertionError("Typed future date accepted");}catch(java.text.ParseException expected){}
  editor.getTextField().setText(LocalDate.now().minusDays(1).toString());
  try{spinner.commitEdit();}catch(java.text.ParseException e){throw new AssertionError(e);}
  if(!((Date)model.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate().equals(LocalDate.now().minusDays(1)))throw new AssertionError("Past date rejected");
  System.out.println("PASS: future arrow, typed date and model updates rejected; past date accepted");
 });}
}
