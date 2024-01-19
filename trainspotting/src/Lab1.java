import TSim.*;

public class Lab1 {

  public Lab1(int speed1, int speed2) {
    
	TSimInterface tsi = TSimInterface.getInstance();
    
	Switch switch1 = new Switch(17,7, tsi);
	Switch switch2 = new Switch(15,9, tsi);
	Switch switch3 = new Switch(4,9, tsi);
	Switch switch4 = new Switch(3,11, tsi);
	
    Train train1 = new Train(1, speed1);
    //Train train2 = new Train(2, speed2);
    
    Thread t1 = new Thread(train1);
    //Thread t2 = new Thread(train2);
    
    
    try {
      tsi.setSpeed(train1.getId(),train1.getSpeed());
      //tsi.setSpeed(train2.getId(), train2.getSpeed());
      /*tsi.setSwitch(17, 7, 0);
      tsi.setSwitch(15, 9, 0);
      tsi.setSwitch(3, 11, 0);*/
      try {
		while(true) {
    	  SensorEvent event = tsi.getSensor(1);
    	  switch1.changeSwitch(event);
    	  switch2.changeSwitch(event);
    	  switch3.changeSwitch(event);
    	  switch4.changeSwitch(event);
    	  System.out.println(event.getXpos() + " " + event.getYpos() + " " + event.getStatus());
		}
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      
      while(true) {
    	  
      }
      
    }
    catch (CommandException e) {
      e.printStackTrace();    // or only e.getMessage() for the error
      System.exit(1);
    }

  }
  
  class Train implements Runnable{
	
	  private int speed;
	  private int id;
	  
	  public Train(int id, int speed) {
		  this.speed = speed;
		  this.id = id;
	  }
	  
	  public int getSpeed() {
		return speed;
		}
	
		public void setSpeed(int speed) {
			this.speed = speed;
		}
	
		public int getId() {
			return id;
		}
	
		public void setId(int id) {
			this.id = id;
		}
	  
	  @Override
	  public void run() {
		  // TODO Auto-generated method stub
	  }
	  
	  public void switchTrack(SensorEvent e) {
		  
	  }
  }
  
  class Switch{
	  
	  final int posX;
	  final int posY;
	  
	  TSimInterface tsi;
	  
	  Sensor westSensor;
	  Sensor eastSensor;
	  Sensor southSensor;
	  
	  public Switch(int posX, int posY, TSimInterface tsi) {
		  this.posX = posX;
		  this.posY = posY;
		  this.tsi = tsi;
		  
		  initSensors();
	  }
	  
	  void initSensors() {
		  westSensor = new Sensor(posX-1, posY);
		  eastSensor = new Sensor(posX+1, posY);
		  southSensor = new Sensor(posX, posY+1);
	  }
	  
	  void changeSwitch(SensorEvent e) {
		  
		  switch(e.getStatus()) {
		  case 1:
			  if(e.getXpos() == westSensor.posX && e.getYpos() == westSensor.posY) {
				  try {
					this.tsi.setSwitch(this.posX, this.posY, 0);
				} catch (CommandException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			  }
			  else if(e.getXpos() == eastSensor.posX && e.getYpos() == eastSensor.posY) {
				  try {
						this.tsi.setSwitch(this.posX, this.posY, 0);
					} catch (CommandException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			  }
			  /*else if(e.getXpos() == southSensor.posX && e.getYpos() == southSensor.posY) {
				  try {
						this.tsi.setSwitch(this.posX, this.posY, 0);
					} catch (CommandException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			  }*/
			  break;
		  case 2:
			  break;
		  }
	  }
  }
  
  class Sensor{
	  
	  final int posX;
	  final int posY;
	  
	  //Status is 2 if inactive, 1 if active
	  int Status;
	  
	  public Sensor(int posX, int posY) {
		  this.posX = posX;
		  this.posY = posY;
		  this.Status = 2;
	  }
	  
	  public void changeStatus(SensorEvent e) {
		  this.Status = e.getStatus();
	  }
  }
}
