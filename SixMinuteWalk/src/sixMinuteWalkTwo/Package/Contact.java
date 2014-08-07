package sixMinuteWalkTwo.Package; 
 
public class Contact {
     
    //private variables
    int _id;
    String _tag;
    String _name;
    int _age;
    String _gender;
    String _des;
    
     
    // Empty constructor
    public Contact(){
         
    }
    // constructor
    public Contact(int id, String tag, String name, int age, String gender, String des){
        this._id = id;
        this._tag = tag;
        this._name = name;
        this._age = age;
        this._gender = gender;
        this._des = des;
    }
    
 // constructor
    public Contact(String tag, String name, int age, String gender, String des){
        this._tag = tag;
        this._name = name;
        this._age = age;
        this._gender = gender;
        this._des = des;
    }
     
    
    // getting ID
    public int getID(){
        return this._id;
    }
     
    // setting id
    public void setID(int id){
        this._id = id;
    }
    
 // getting name
    public String getTag(){
        return this._tag;
    }
     
    // setting name
    public void setTag(String tag){
        this._tag = tag;
    }
     
    // getting name
    public String getName(){
        return this._name;
    }
     
    // setting name
    public void setName(String name){
        this._name = name;
    }
     
    // getting phone number
    public int getAge(){
        return this._age;
    }
     
    // setting phone number
    public void setAge(int age){
        this._age = age;
    }
    
 // getting name
    public String getGender(){
        return this._gender;
    }
     
    // setting name
    public void setGender(String gender){
        this._gender = gender;
    }
    
 // getting name
    public String getDes(){
        return this._des;
    }
     
    // setting name
    public void setDes(String des){
        this._des = des;
    }
}

/*package rFIDDatabaseWiFi.Package; 
 
public class Contact {
     
    //private variables
    int _id;
    String _name;
    String _phone_number;
     
    // Empty constructor
    public Contact(){
         
    }
    // constructor
    public Contact(int id, String name, String _phone_number){
        this._id = id;
        this._name = name;
        this._phone_number = _phone_number;
    }
     
    // constructor
    public Contact(String name, String _phone_number){
        this._name = name;
        this._phone_number = _phone_number;
    }
    // getting ID
    public int getID(){
        return this._id;
    }
     
    // setting id
    public void setID(int id){
        this._id = id;
    }
     
    // getting name
    public String getName(){
        return this._name;
    }
     
    // setting name
    public void setName(String name){
        this._name = name;
    }
     
    // getting phone number
    public String getPhoneNumber(){
        return this._phone_number;
    }
     
    // setting phone number
    public void setPhoneNumber(String phone_number){
        this._phone_number = phone_number;
    }
}*/