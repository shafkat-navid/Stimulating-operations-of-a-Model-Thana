package com.example.modelthana.Navid;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
public class RecordRow {
 private final StringProperty id, operation, subject, location, date, status, details;
 public RecordRow(String id, String operation, String subject, String location, String date, String status) {
  this(id, operation, subject, location, date, status, "");
 }
 public RecordRow(String id, String operation, String subject, String location, String date, String status, String details) {
  this.id=new SimpleStringProperty(id); this.operation=new SimpleStringProperty(operation);
  this.subject=new SimpleStringProperty(subject); this.location=new SimpleStringProperty(location);
  this.date=new SimpleStringProperty(date); this.status=new SimpleStringProperty(status);
  this.details=new SimpleStringProperty(details == null ? "" : details);
 }
 public String getId(){return id.get();}
 public String getOperation(){return operation.get();}
 public String getSubject(){return subject.get();}
 public String getLocation(){return location.get();}
 public String getDate(){return date.get();}
 public String getStatus(){return status.get();}
 public void setStatus(String value){status.set(value);}
 public String getDetails(){return details.get();}
 public StringProperty idProperty(){return id;}
 public StringProperty operationProperty(){return operation;}
 public StringProperty subjectProperty(){return subject;}
 public StringProperty locationProperty(){return location;}
 public StringProperty dateProperty(){return date;}
 public StringProperty statusProperty(){return status;}
 public StringProperty detailsProperty(){return details;}
}
