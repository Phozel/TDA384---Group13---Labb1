import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1 {

  public Lab1(int speed1, int speed2) {
    
	TSimInterface tsi = TSimInterface.getInstance();
	
    Train train1 = new Train(1, speed1, tsi);
    Train train2 = new Train(2, speed2, tsi);
    
    Thread t1 = new Thread(train1);
    Thread t2 = new Thread(train2);
    
    //TrainThread trainThread1 = new TrainThread(train1, t1);
    //TrainThread trainThread2 = new TrainThread(train2, t2);
    
    ArrayList trainList = new ArrayList();
    trainList.add(train1);
    trainList.add(train2);
    
    Semaphore startSem1 = new Semaphore(1);
    Semaphore startSem2 = new Semaphore(1);
    Semaphore startSem3 = new Semaphore(1);
    Semaphore startSem4 = new Semaphore(1);
    
    Semaphore switchSem1 = new Semaphore(1);
    Semaphore switchSem2 = new Semaphore(1);
    Semaphore switchSem3 = new Semaphore(1);
    Semaphore switchSem4 = new Semaphore(1);
    
    ArrayList<Semaphore> switchNeighbor1 = new ArrayList<>(
    		Arrays.asList(switchSem2));
    ArrayList<Semaphore> switchNeighbor2 = new ArrayList<>(
    		Arrays.asList(switchSem1, switchSem3));
    ArrayList<Semaphore> switchNeighbor3 = new ArrayList<>(
    		Arrays.asList(switchSem2, switchSem4));
    ArrayList<Semaphore> switchNeighbor4 = new ArrayList<>(
    		Arrays.asList(switchSem3)); 
    
    Terminal terminal1 = new Terminal (tsi, 16, 3, startSem1, trainList, switchSem1);
	Terminal terminal2 = new Terminal (tsi, 16, 5, startSem2, trainList, switchSem1);
	Terminal terminal3 = new Terminal (tsi, 16, 11, startSem3, trainList, switchSem4);
	Terminal terminal4 = new Terminal (tsi, 16, 13, startSem4, trainList, switchSem4);
    
	
	ArrayList<Terminal> terminalList1 = new ArrayList<>(
			Arrays.asList(terminal1, terminal2));
	ArrayList<Terminal> terminalList2 = new ArrayList<>(
			Arrays.asList(terminal3, terminal4));
	ArrayList<Terminal> terminalList3 = new ArrayList<>(
			Arrays.asList(terminal1, terminal2, terminal3, terminal4));
    
    Switch switch1 = new Switch(1, 17,7, 1, tsi, switchSem1, trainList, switchNeighbor1, terminalList1);
	Switch switch2 = new Switch(2, 15,9, 1, tsi, switchSem2, trainList, switchNeighbor2, null);
	Switch switch3 = new Switch(3, 4,9, 0, tsi, switchSem3, trainList, switchNeighbor3, null);
	Switch switch4 = new Switch(4, 3,11, 0, tsi, switchSem4, trainList, switchNeighbor4, terminalList2);
	
	ArrayList<Switch> switchList = new ArrayList<>(
			Arrays.asList(switch1, switch2, switch3, switch4));
	
	
	
	train1.acquireSwitches(switchList);
	train2.acquireSwitches(switchList);
	train1.acquireTerminals(terminalList3);
	train2.acquireTerminals(terminalList3);
	
	t1.start();
    t2.start();
	
    try {
      tsi.setSpeed(train1.getId(),train1.getSpeed());
      tsi.setSpeed(train2.getId(), train2.getSpeed());
      tsi.setSwitch(17,7, 1);
  	  tsi.setSwitch(15,9, 1);
  	  tsi.setSwitch(4,9, 0);
  	  tsi.setSwitch(3,11, 0);
    
    }
    catch (CommandException e) {
      e.printStackTrace();    // or only e.getMessage() for the error
      System.exit(1);
    }
    
    

  }
  
  class Train implements Runnable{
	
	  private volatile int speed;
	  private int maxSpeed = 50;
	  private int id;
	  TSimInterface tsi;
	  private ArrayList<Switch> switchList;
	  private ArrayList<Terminal> terminalList;
	  int semCounter = 0;
	  
	  boolean hasStarted = false;
	  
	  public Train(int id, int speed, TSimInterface tsi) {
		  this.speed = speed;
		  this.id = id;
		  this.tsi = tsi;
	  }
	  
	  public int getSpeed() {
		return speed;
		}
	
		public void brake() {
			try {
				this.tsi.setSpeed(this.id, 0);
			} catch (CommandException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
		public int getId() {
			return id;
		}
	
		public void setId(int id) {
			this.id = id;
		}
		
		public boolean getHasStarted() {
			return this.hasStarted;
		}
		
		public void setHasStarted(boolean x) {
			this.hasStarted = x;
		}
		
		public void acquireSwitches(ArrayList<Switch> switchList) {
			this.switchList = switchList;
		}
		
		public void acquireTerminals(ArrayList<Terminal> terminalList) {
			this.terminalList = terminalList;
		}
		
		public void incSemCounter() {
			this.semCounter++;
		}
		
		public boolean checkSemCounter() {
			return this.semCounter < 1;
		}
	  
	  @Override
	  public void run() {
		  // TODO Auto-generated method stub
		while(true) {
		  SensorEvent event;
		  /*if(speed < maxSpeed) {
			  speed = maxSpeed;
			  try {
				tsi.getInstance().setSpeed(this.id, speed);
			} catch (CommandException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  }*/
		try {
			event = tsi.getSensor(this.id);
			int temp[] = new int[] {event.getXpos(), event.getYpos()};
			
			for(int i = 0; i<4; i++) {
				Set<int[]> keySet = switchList.get(i).getSensorMap().keySet();
				for(int[] key : keySet) {
					if (key[0] == temp[0] && key[1] == temp[1]) {
						switchList.get(i).onSensorEvent(event);
					}
				}
			}
			
			for(Terminal terminal : terminalList) {
				if(terminal.getPos()[0] == temp[0] && terminal.getPos()[1] == temp[1])
					terminal.onTerminalSensorEvent(event);
			}
			
		} catch (CommandException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	  }
	  }
	  }
  
  class Switch{
	  
	  final int id;
	  final int posX;
	  final int posY;
	  
	  TSimInterface tsi;
	  
	  Sensor westSensor;
	  Sensor eastSensor;
	  Sensor southSensor;
	  
	  HashMap<int[], Sensor> sensorMap = new HashMap<>();
	  
	  Semaphore switchSem;
	  
	  boolean hasActiveSensor = false;
	  int sensorCounter = 0;
	  
	  int orientation;
	  ArrayList<Train> trainList;
	  ArrayList<Semaphore> switchNeighList;
	  ArrayList<Terminal> terminalList;
	  int neighborAmount;
	  
	  public Switch(int id, int posX, int posY, int ori, 
			  TSimInterface tsi, Semaphore switchSem, ArrayList<Train> trainList, 
			  ArrayList<Semaphore> switchNeighbor, ArrayList<Terminal> terminalList) {
		  this.id = id;
		  this.posX = posX;
		  this.posY = posY;
		  this.orientation = ori;
		  this.tsi = tsi;
		  this.switchSem = switchSem;
		  this.trainList = trainList;
		  this.switchNeighList = switchNeighbor;
		  this.neighborAmount  = switchNeighList.size();
		  this.terminalList = terminalList;
		  
		  initSensors();
	  }
	  
	  void initSensors() {
		  westSensor = new Sensor(posX-1, posY);
		  eastSensor = new Sensor(posX+1, posY);
		  southSensor = new Sensor(posX, posY+1);
		  
		  sensorMap.put(this.westSensor.getPosList(), this.westSensor);
		  sensorMap.put(this.eastSensor.getPosList(), this.eastSensor);
		  sensorMap.put(this.southSensor.getPosList(), this.southSensor);
	  }
	  
	  public HashMap getSensorMap() {
		  return this.sensorMap;
	  }
	  
	  void onSensorEvent(SensorEvent e) {
		//System.out.println("onSensorEvent reached");
		
		/*
		 * To-do, remove the following line of code: switchSem.tryAcquire();
		 * */
		switchSem.tryAcquire();
		changeSwitch(e);
		//}
		//else {
			//stopTrain(e.getTrainId());
		//}
		

	  }
	  
	  private void stopTrain(int trainId) {
		  System.out.println("stop train reached");
		  this.trainList.get(trainId-1).brake();
	}
	  
	private void goSouth(int trainId) {
		try {
			this.tsi.setSwitch(this.posX, this.posY, this.orientation * -1 + 1);
		} catch (CommandException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void changeSwitch(SensorEvent e) {
		
		//System.out.println("ChangeSwitch reached");
		// East -> West orientation  
		boolean hasTerminal = this.terminalList != null;
		System.out.println(hasTerminal);
		
		if(this.orientation == 1) {
			  if(e.getXpos() == westSensor.posX && e.getYpos() == westSensor.posY) {
				  try {
						System.out.println(switchNeighList.get(0));
					  
					  if(switchNeighList.get(0).tryAcquire()) {
						  System.out.println("Acquired semaphore");
						  sensorCounter++;
						  this.tsi.setSwitch(this.posX, this.posY, 0);
						  
					  }
					  else {
						  //if(!(eastSensor.getStatus() == 1 || southSensor.getStatus() == 1))
							  //stopTrain(e.getTrainId());
					  }
					  checkSensorCounter(1);
				} catch (CommandException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			  }
			  else if(e.getXpos() == eastSensor.posX && e.getYpos() == eastSensor.posY) {
				  
				  try {
					  if(sensorCounter <= 2 && hasTerminal) {
						  if(terminalList.get(0).getTermSem().tryAcquire()) {
							  this.tsi.setSwitch(posX, posY, 0);
						  }
						  else {
							  terminalList.get(1).getTermSem().tryAcquire();
							  this.tsi.setSwitch(this.posX, this.posX, 1);
						  }
					  }
					  else if(switchNeighList.get(neighborAmount-1).tryAcquire()) {
							System.out.println("Acquired semaphore");
							sensorCounter++;
							this.tsi.setSwitch(this.posX, this.posY, 0);
						
					  }
					  else {
						  goSouth(e.getTrainId());
					  }
					} catch (CommandException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				  checkSensorCounter(0);
			  }
			  sensorCounter++;
		  }
		  // West -> East orientation
		  else if(this.orientation == 0) {
			  if(e.getXpos() == westSensor.posX && e.getYpos() == westSensor.posY) {
				  try {
					  if(sensorCounter <= 2 && hasTerminal) {
						  if(terminalList.get(0).getTermSem().tryAcquire()) {
							  this.tsi.setSwitch(posX, posY, 0);
						  }
						  else {
							  System.out.println("Hejooooo");
							  terminalList.get(1).getTermSem().tryAcquire();
							  this.tsi.setSwitch(posX, posX, 1);
						  }
					  }
					  else if(switchNeighList.get(0).tryAcquire()) {
						  System.out.println("Acquired semaphore");
						  sensorCounter++;
						  this.tsi.setSwitch(this.posX, this.posY, 1);  
						  
					  }
					  else {
						  goSouth(e.getTrainId());
						  
					  }
				} catch (CommandException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				  checkSensorCounter(1);
			  }
			  else if(e.getXpos() == eastSensor.posX && e.getYpos() == eastSensor.posY) {
				  try {
					  
						  if(switchNeighList.get(neighborAmount-1).tryAcquire()) {
							  System.out.println("Acquired semaphore");
							  sensorCounter++;
							this.tsi.setSwitch(this.posX, this.posY, 1);
							
						  }
						  else {
							  //if(!(westSensor.getStatus() == 1 || southSensor.getStatus() == 1))
								  //stopTrain(e.getTrainId());
						  }
					} catch (CommandException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				  checkSensorCounter(0);
			  }
		  }
		
	  }
	
	public void checkSensorCounter(int i) {
		if (sensorCounter == 4) {
			if(neighborAmount > 1) {
				this.switchNeighList.get(i).release();
				System.out.println("Released semaphore");
				sensorCounter = 0;
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
	  
	  public int getStatus() {
		  return this.Status;
	  }
	  
	  public int[] getPosList() {
		  int temp[] = new int[] {this.posX, this.posY};
		  
		  return temp;
	  }
  }
  
class Terminal{
	  
	  TSimInterface tsi;
	  Sensor sensor;
	  	  
	  Semaphore terminalSem;
	  Semaphore neighSem;
	  
	  ArrayList<Train> trainList;
	  
	  public Terminal(TSimInterface tsi, int posX, int posY, Semaphore terminalSem, ArrayList<Train> trainList, Semaphore neighSem) {

		  this.tsi = tsi;
		  this.terminalSem = terminalSem;
		  this.trainList = trainList;
		  this.neighSem = neighSem;
		  
		  terminalSem.tryAcquire();
		  initSensor(posX, posY);
	  }
	  
	  public int[] getPos() {
		  int temp[] = sensor.getPosList();
		  
		  return temp;
	  }
	  
	  public Semaphore getTermSem() {
		  return this.terminalSem;
	  }
	  
	  void initSensor(int posX, int posY) {
		  this.sensor = new Sensor (posX, posY);
	  }
	  
	
	  
	  void onTerminalSensorEvent(SensorEvent e) {
		  System.out.println("hej1");
		  
		  boolean hasStarted = trainList.get(e.getTrainId()-1).getHasStarted();
		  
		  if(!hasStarted && e.getStatus() == 1) {
			  startTrain(e.getTrainId());
			  System.out.println("hej2");
		  }
		  else if (e.getStatus() == 2 && !hasStarted) {
			  trainList.get(e.getTrainId()-1).setHasStarted(true);
			  System.out.println("hej3");
		  }
		  else {
			  System.out.println("hej");
			  stopTrain(e.getTrainId());
			  wait2Seconds(e.getTrainId());
			  startTrain(e.getTrainId());
		  }

	  }

	  void wait2Seconds(int e) {
		  try {
			this.trainList.get(e-1).wait(2000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		};
		  
	  }
	  
	  private void stopTrain(int trainId) {
		  System.out.println("stop train reached");
		  this.trainList.get(trainId-1).brake();
	  }
	  
	  private void startTrain(int trainId) {
	  	  // TODO Auto-generated method stub
			
	  }
} 
}
