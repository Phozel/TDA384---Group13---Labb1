import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1 {

  public Lab1(int speed1, int speed2) {
    
	TSimInterface tsi = TSimInterface.getInstance();
	
    Train train1 = new Train(1, speed1);
    Train train2 = new Train(2, speed2);
    ArrayList trainList = new ArrayList();
    trainList.add(train1);
    trainList.add(train2);
    
    Thread t1 = new Thread(train1);
    Thread t2 = new Thread(train2);
    
    Semaphore startSem1 = new Semaphore(1);
    Semaphore startSem2 = new Semaphore(1);
    Semaphore stationSem1 = new Semaphore(1);
    Semaphore stationSem2 = new Semaphore(1);
    
    Semaphore switchSem1 = new Semaphore(1);
    Semaphore switchSem2 = new Semaphore(1);
    Semaphore switchSem3 = new Semaphore(1);
    Semaphore switchSem4 = new Semaphore(1);
    
    Switch switch1 = new Switch(17,7, 1, tsi, switchSem1, trainList);
	Switch switch2 = new Switch(15,9, 1, tsi, switchSem2, trainList);
	Switch switch3 = new Switch(4,9, 0, tsi, switchSem3, trainList);
	Switch switch4 = new Switch(3,11, 0, tsi, switchSem4, trainList);
    
    try {
      tsi.setSpeed(train1.getId(),train1.getSpeed());
      tsi.setSpeed(train2.getId(), train2.getSpeed());
      tsi.setSwitch(17,7, 0);
  	  tsi.setSwitch(15,9, 0);
  	  tsi.setSwitch(4,9, 0);
  	  tsi.setSwitch(3,11, 0);
      try {
		while(true) {
			int x = 1;
			if (x == 0) {
	    	  SensorEvent event = tsi.getSensor(1);
	    	  switch1.onSensorEvent(event);
	    	  switch2.onSensorEvent(event);
	    	  switch3.onSensorEvent(event);
	    	  switch4.onSensorEvent(event);
	    	  x++;
			}
			else if (x == 1) {
	    	  SensorEvent event2 = tsi.getSensor(2);
	    	  switch1.onSensorEvent(event2);
	    	  switch2.onSensorEvent(event2);
	    	  switch3.onSensorEvent(event2);
	    	  switch4.onSensorEvent(event2);
	    	  x--;
			}
    	  
    	  //System.out.println(event.getXpos() + " " + event.getYpos() + " " + event.getStatus());
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
		}		// TODO Auto-generated method stub

	  
	  @Override
	  public void run() {
		  // TODO Auto-generated method stub
	  }
  }
  
  class Switch{
	  
	  
	  final int posX;
	  final int posY;
	  
	  TSimInterface tsi;
	  
	  Sensor westSensor;
	  Sensor eastSensor;
	  Sensor southSensor;
	  
	  Semaphore switchSem;
	  
	  boolean hasActiveSensor = false;
	  int orientation;
	  ArrayList trainList;
	  
	  public Switch(int posX, int posY, int ori, TSimInterface tsi, Semaphore switchSem, ArrayList trainList) {
		  this.posX = posX;
		  this.posY = posY;
		  this.orientation = ori;
		  this.tsi = tsi;
		  this.switchSem = switchSem;
		  this.trainList = trainList;
		  
		  initSensors();
	  }
	  
	  void initSensors() {
		  westSensor = new Sensor(posX-1, posY);
		  eastSensor = new Sensor(posX+1, posY);
		  southSensor = new Sensor(posX, posY+1);
	  }
	  
	  void onSensorEvent(SensorEvent e) {
		  

		
		if(switchSem.tryAcquire()) {	  
			this.hasActiveSensor = true;
			changeSwitch(e);
			this.hasActiveSensor = false;
			switchSem.release();
		}
		else {
			stopTrain(e.getTrainId());
		}
		

	  }
	  
	  private void stopTrain(int trainId) {
		  int temp = trainId -1;
		  ((Lab1.Train) this.trainList.get(temp)).setSpeed(0);
	}

	void changeSwitch(SensorEvent e) {
		  if(this.orientation == 1) {
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
					  System.out.println("is wrong");
						this.tsi.setSwitch(this.posX, this.posY, 0);
					} catch (CommandException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			  }
		  }
		  else if(this.orientation == 0) {
			  if(e.getXpos() == westSensor.posX && e.getYpos() == westSensor.posY) {
				  try {
					  this.tsi.setSwitch(this.posX, this.posY, 1);
				} catch (CommandException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			  }
			  else if(e.getXpos() == eastSensor.posX && e.getYpos() == eastSensor.posY) {
				  try {
					  System.out.println("Should be correct");
						this.tsi.setSwitch(this.posX, this.posY, 1);
					} catch (CommandException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			  }
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
