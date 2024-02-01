import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1 {
	
	Thread t1;
	Thread t2;

	public Lab1(int speed1, int speed2) {

		TSimInterface tsi = TSimInterface.getInstance();

		Train train1 = new Train(1, speed1, tsi);
		Train train2 = new Train(2, speed2, tsi);

		//t1 = new Thread(train1);
		//t2 = new Thread(train2);

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

		ArrayList<Semaphore> switchNeighbor1 = new ArrayList<>(Arrays.asList(switchSem2));
		ArrayList<Semaphore> switchNeighbor2 = new ArrayList<>(Arrays.asList(switchSem1, switchSem3));
		ArrayList<Semaphore> switchNeighbor3 = new ArrayList<>(Arrays.asList(switchSem2, switchSem4));
		ArrayList<Semaphore> switchNeighbor4 = new ArrayList<>(Arrays.asList(switchSem3));

		Terminal terminal1 = new Terminal(tsi, 16, 3, startSem1, trainList, switchSem1);
		Terminal terminal2 = new Terminal(tsi, 16, 5, startSem2, trainList, switchSem1);
		Terminal terminal3 = new Terminal(tsi, 16, 11, startSem3, trainList, switchSem4);
		Terminal terminal4 = new Terminal(tsi, 16, 13, startSem4, trainList, switchSem4);

		ArrayList<Terminal> terminalList1 = new ArrayList<>(Arrays.asList(terminal1, terminal2));
		ArrayList<Terminal> terminalList2 = new ArrayList<>(Arrays.asList(terminal3, terminal4));
		ArrayList<Terminal> terminalList3 = new ArrayList<>(Arrays.asList(terminal1, terminal2, terminal3, terminal4));

		Switch switch1 = new Switch(1, 17, 7, 1, tsi, switchSem1, trainList, switchNeighbor1, terminalList1);
		Switch switch2 = new Switch(2, 15, 9, 1, tsi, switchSem2, trainList, switchNeighbor2, null);
		Switch switch3 = new Switch(3, 4, 9, 0, tsi, switchSem3, trainList, switchNeighbor3, null);
		Switch switch4 = new Switch(4, 3, 11, 0, tsi, switchSem4, trainList, switchNeighbor4, terminalList2);

		ArrayList<Switch> switchList = new ArrayList<>(Arrays.asList(switch1, switch2, switch3, switch4));
		
		ArrayList<Switch> switch1NeighList = new ArrayList<>(Arrays.asList(switch2));
		ArrayList<Switch> switch2NeighList = new ArrayList<>(Arrays.asList(switch1, switch3));
		ArrayList<Switch> switch3NeighList = new ArrayList<>(Arrays.asList(switch2, switch4));
		ArrayList<Switch> switch4NeighList = new ArrayList<>(Arrays.asList(switch3));
		
		switch1.initSwitchNeigh(switch1NeighList);
		switch2.initSwitchNeigh(switch2NeighList);
		switch3.initSwitchNeigh(switch3NeighList);
		switch4.initSwitchNeigh(switch4NeighList);
		
		train1.acquireSwitches(switchList);
		train2.acquireSwitches(switchList);
		train1.acquireTerminals(terminalList3);
		train2.acquireTerminals(terminalList3);

		
		train1.start();
		train2.start();

		try {
			tsi.setSpeed(train1.getTrainId(), train1.getSpeed());
			tsi.setSpeed(train2.getTrainId(), train2.getSpeed());
			tsi.setSwitch(17, 7, 1);
			tsi.setSwitch(15, 9, 1);
			tsi.setSwitch(4, 9, 0);
			tsi.setSwitch(3, 11, 0);

		} catch (CommandException e) {
			e.printStackTrace(); // or only e.getMessage() for the error
			System.exit(1);
		}
		
		
	}
	
	class Train extends Thread {

		private volatile int speed;
		private int maxSpeed = 20; // TODO: Byt 
		private int id;
		volatile int sensorCounter = 0;
		
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

		public void incSensorCounter() {
			sensorCounter++;
		}
		
		public void resetSensorCounter() {
			sensorCounter = 0;
		}
		
		public void checkSensorCounter() {
			if(sensorCounter == 4)
				resetSensorCounter();
		}
		
		public int getSpeed() {
			return speed;
		}
		
		public void setSpeed(int newSpeed) {
			try {
				this.speed = newSpeed;
				this.tsi.setSpeed(this.id, speed);
			} catch (CommandException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void brake() {
			try {
				this.tsi.setSpeed(this.id, 0);
				this.speed = 0;
			} catch (CommandException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public int getTrainId() {
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

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (true) {
				SensorEvent event;
				/*
				 * if(speed < maxSpeed) { speed = maxSpeed; try {
				 * tsi.getInstance().setSpeed(this.id, speed); } catch (CommandException e) { //
				 * TODO Auto-generated catch block e.printStackTrace(); } }
				 */
				try {
					event = tsi.getSensor(this.id);
					int ePos[] = new int[] { event.getXpos(), event.getYpos() };

					for (int i = 0; i < 4; i++) {
						Set<int[]> keySet = switchList.get(i).getSensorMap().keySet();
						for (int[] key : keySet) {
							if (key[0] == ePos[0] && key[1] == ePos[1]) {
								this.incSensorCounter(); //ksk inte behöver vara this.
								switchList.get(i).onSensorEvent(event, this.sensorCounter);
								this.checkSensorCounter();
							}
						}
					}

					for (Terminal terminal : terminalList) {
						if (terminal.getPos()[0] == ePos[0] && terminal.getPos()[1] == ePos[1])
							terminal.onTerminalSensorEvent(event);
					}

				} catch (CommandException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	}

	class Switch {

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
		volatile int sensorCounter;

		int orientation;
		ArrayList<Train> trainList;
		ArrayList<Semaphore> switchNeighSemList;
		ArrayList<Switch> switchNeighList;
		ArrayList<Terminal> terminalList;
		int neighborAmount;
		Semaphore[] semaphoreToRelease = new Semaphore[2];
		
		int oldSpeed;

		public Switch(int id, int posX, int posY, int ori, TSimInterface tsi, Semaphore switchSem,
				ArrayList<Train> trainList, ArrayList<Semaphore> switchNeighbor, 
				ArrayList<Terminal> terminalList) {
			this.id = id;
			this.posX = posX;
			this.posY = posY;
			this.orientation = ori;
			this.tsi = tsi;
			this.switchSem = switchSem;
			this.trainList = trainList;
			this.switchNeighSemList = switchNeighbor;
			
			this.neighborAmount = switchNeighSemList.size();
			this.terminalList = terminalList;

			initSensors();
		}

		void initSensors() {
			westSensor = new Sensor(posX - 1, posY);
			eastSensor = new Sensor(posX + 1, posY);
			southSensor = new Sensor(posX, posY + 1);

			sensorMap.put(this.westSensor.getPosList(), this.westSensor);
			sensorMap.put(this.eastSensor.getPosList(), this.eastSensor);
			sensorMap.put(this.southSensor.getPosList(), this.southSensor);
		}

		public HashMap<int[], Sensor> getSensorMap() {
			return this.sensorMap;
		}
		
		public void initSwitchNeigh(ArrayList<Switch> switchNeighList){
			this.switchNeighList = switchNeighList;
		}

		void onSensorEvent(SensorEvent e, int sensorCounter) {
			this.sensorCounter = sensorCounter;
			//System.out.println("detta är sensor counter: " +  this.sensorCounter);
			changeSwitch(e);
		}

		private void stopTrain(int trainId) {
			System.out.println("stop train reached");
			oldSpeed = this.trainList.get(trainId - 1).getSpeed();
			this.trainList.get(trainId - 1).brake();
			this.switchSem.release();
		}
		
		private void startTrain(int tID) {
			System.out.println(this.oldSpeed);
			this.trainList.get(tID - 1).setSpeed(this.oldSpeed);
		}

		private void goSouth(int trainId) {
			try {
				this.tsi.setSwitch(this.posX, this.posY, this.orientation);
				//this.tsi.setSwitch(this.posX, this.posY, this.orientation * -1 + 1);
				semaphoreToRelease[1] = this.switchSem;
			} catch (CommandException e) {
				// TODO Auto-generated catch block
				System.out.println("rad 263 " + this.posX + " " + this.posY);
				e.printStackTrace();
			}
			//checkSensorCounter(0);
		}

		void changeSwitch(SensorEvent e) {
			
			// System.out.println("ChangeSwitch reached");
			
			boolean hasTerminal = this.terminalList != null;
			//System.out.println("hasTerminal? " + hasTerminal);
			//int semaphoreToRelease = 10; //just some number
			//Semaphore semaphoreToRelease = null;
			
			//semaphoreToRelease[0] = null;
			//semaphoreToRelease[1] = null;
			if (this.sensorCounter < 2) {
				// East -> West orientation
				if (this.orientation == 1) {
					
					if (e.getXpos() == westSensor.posX && e.getYpos() == westSensor.posY) {
						westSensor.changeStatus(e);
						try {
							System.out.println("rad 297 " + switchNeighSemList.get(0));

							if (switchNeighSemList.get(0).tryAcquire()) {
								
								System.out.println("Acquired semaphore");
								this.tsi.setSwitch(this.posX, this.posY, 0);

							} else {//if the train has only activated one of the switches sensors				
									stopTrain(e.getTrainId());

							}
							if(!(hasTerminal)) {
								semaphoreToRelease[0] = switchNeighSemList.get(1);
								semaphoreToRelease[1] = this.switchSem;
								//semaphoreToRelease = switchNeighList.get(1);
								
							} else{
								semaphoreToRelease[0] = this.switchSem;
								//semaphoreToRelease = this.switchSem;
								//semaphoreToRelease = 2; //2 means to release self/the current switches semaphore
							}
						} catch (CommandException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					} else if (e.getXpos() == eastSensor.posX && e.getYpos() == eastSensor.posY) {
						eastSensor.changeStatus(e);
						try {
							if (hasTerminal) {
								if (terminalList.get(0).getTermSem().tryAcquire()) {
									this.tsi.setSwitch(posX, posY, 0);
								} else if(terminalList.get(1).getTermSem().tryAcquire()) {
									this.tsi.setSwitch(this.posX, this.posX, 1);
								}
							} else if (switchNeighSemList.get(neighborAmount - 1).tryAcquire()) {
								System.out.println("Acquired semaphore");
								this.tsi.setSwitch(this.posX, this.posY, 0);
							} else {
								System.out.println("shoud go south");
								goSouth(e.getTrainId());
							}
						} catch (CommandException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
							System.out.println("rad 341 " + this.posX + " " + this.posY);
						}
						semaphoreToRelease[0] = switchNeighSemList.get(0);
					}
					else if (e.getXpos() == southSensor.posX && e.getYpos() == southSensor.posY){ //crashes here now for some reason
						southSensor.changeStatus(e);
						try {
						 if (switchNeighSemList.get(neighborAmount - 1).tryAcquire()) {
							System.out.println("Acquired semaphore");
							this.tsi.setSwitch(this.posX, this.posY, 1);
						} else {
							if (switchNeighList.get(neighborAmount - 1).getActiveSensor() != null)
								stopTrain(e.getTrainId());
						}
						
						} catch (CommandException e1) {
							System.out.println("rad 345 " + this.posX + " " + this.posY);
							e1.printStackTrace();
						}
						if (hasTerminal) {
							semaphoreToRelease[0] = terminalList.get(1).getTermSem();
							semaphoreToRelease[1] = this.switchSem;
						}
						/*else {
							semaphoreToRelease[0] = switchNeighList.get(1);
						}*/
					}
				}
				// West -> East orientation
				else if (this.orientation == 0) {
					if (e.getXpos() == westSensor.posX && e.getYpos() == westSensor.posY) {
						westSensor.changeStatus(e);
						try {
							if (hasTerminal) {
								System.out.println("ska välja en terminal");
								System.out.println(this.terminalList.get(1).getTermSem());
								int termViTar = -1;
								if (this.terminalList.get(0).getTermSem().tryAcquire()) {
									this.tsi.setSwitch(posX, posY, 1);
									termViTar = 0;
								} else if(this.terminalList.get(1).getTermSem().tryAcquire()){
									this.tsi.setSwitch(posX, posX, 0);
									termViTar = 1;
								}
								System.out.println("terminal " + termViTar);
							} else if (switchNeighSemList.get(0).tryAcquire()) {
								System.out.println("Acquired semaphore");
								this.tsi.setSwitch(this.posX, this.posY, 1);
							} else {
								goSouth(e.getTrainId());
							}
						} catch (CommandException e1) {
							// TODO Auto-generated catch block
							System.out.println("rad 373 " + this.posX + " " + this.posY);
							e1.printStackTrace();
						}
						semaphoreToRelease[0] = switchNeighSemList.get(neighborAmount - 1);
					} else if (e.getXpos() == eastSensor.posX && e.getYpos() == eastSensor.posY) {
						eastSensor.changeStatus(e);
						try {
							if (switchNeighSemList.get(neighborAmount - 1).tryAcquire()) {
								System.out.println("Acquired semaphore");
								this.tsi.setSwitch(this.posX, this.posY, 1);

							} else { //if the train has only activated one the switches sensors
								stopTrain(e.getTrainId());
							}
						} catch (CommandException e1) {
							// TODO Auto-generated catch block
							//System.out.println("rad 393 " + this.posX + " " + this.posY);
							e1.printStackTrace();
						}
						if(hasTerminal) {
							semaphoreToRelease[0] = terminalList.get(0).getTermSem();
							semaphoreToRelease[1] = this.switchSem;
							System.out.println("released terminal at" + terminalList.get(0).getPos()[1]);
						} else {
							semaphoreToRelease[0] = switchNeighSemList.get(0);
						}
						
					} else if (e.getXpos() == southSensor.posX && e.getYpos() == southSensor.posY){
						southSensor.changeStatus(e);
						try {
							 if (switchNeighSemList.get(neighborAmount - 1).tryAcquire()) {
								System.out.println("Acquired semaphore");
								this.tsi.setSwitch(this.posX, this.posY, 0);
							} else {
								stopTrain(e.getTrainId());
							}
						} catch (CommandException e1) {
							e1.printStackTrace();
						}
						if(hasTerminal) {
							semaphoreToRelease[0] = terminalList.get(1).getTermSem();
							semaphoreToRelease[1] = this.switchSem;
						} /*else {
							semaphoreToRelease[0] = switchNeighList.get(0);
						}*/
					}
				}
			}
			newCheckSensorCounter(semaphoreToRelease);
			System.out.println("sensorCounter " + sensorCounter);
		}

		public void newCheckSensorCounter(Semaphore[] semsToRelease) {
			if (sensorCounter == 4) {		
				for (Semaphore sem : semsToRelease) {
					if (sem != null) {
						sem.release();	
					}
				}
				if(getActiveSensor() != null) {
						System.out.println(this.trainList.get(getActiveSensor().getLatestEvent().getTrainId()-1).getSpeed());
						if(this.trainList.get(getActiveSensor().getLatestEvent().getTrainId()-1).getSpeed() == 0) {
							System.out.println("Found Active Sensor");
							try {
								if (this.orientation == 1) {
									this.tsi.setSwitch(this.posX, this.posY, 0);
								} else if (this.orientation == 0) {
									this.tsi.setSwitch(this.posX, this.posY, 1);
								}
							} catch (CommandException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							startTrain(getActiveSensor().getLatestEvent().getTrainId());
					}
				}
				semaphoreToRelease[0] = null;
				semaphoreToRelease[1] = null;
			
			}
		}
		
		public Sensor getActiveSensor() {
			
			if(this.westSensor.getStatus() == 1) {
				return this.westSensor;
			}else if (this.eastSensor.getStatus() == 1) {
				return this.eastSensor;
			}else if (this.southSensor.getStatus() == 1) {
				return this.southSensor;
			}
			else {
				return null;
			}
			
		}
		
		
		public void releaseSelf() {
			this.switchSem.release();
			System.out.println("Released own semaphore");
		}
	}

	class Sensor {

		final int posX;
		final int posY;
		SensorEvent latestEvent;
		
		// Status is 2 if inactive, 1 if active
		int Status;

		public Sensor(int posX, int posY) {
			this.posX = posX;
			this.posY = posY;
			this.Status = 2;
		}

		public void changeStatus(SensorEvent e) {
			this.Status = e.getStatus();
			latestEvent = e;
		}

		public int getStatus() {
			return this.Status;
		}

		public int[] getPosList() {
			int temp[] = new int[] { this.posX, this.posY };

			return temp;
		}
		
		public SensorEvent getLatestEvent() {
			return latestEvent;
		}
		
	}

	class Terminal {

		TSimInterface tsi;
		Sensor sensor;

		Semaphore terminalSem;
		Semaphore neighSem;

		ArrayList<Train> trainList;

		public Terminal(TSimInterface tsi, int posX, int posY, Semaphore terminalSem, ArrayList<Train> trainList,
				Semaphore neighSem) {

			this.tsi = tsi;
			this.terminalSem = terminalSem;
			this.trainList = trainList;
			this.neighSem = neighSem;

			terminalSem.tryAcquire();
			neighSem.tryAcquire();
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
			this.sensor = new Sensor(posX, posY);
		}

		void onTerminalSensorEvent(SensorEvent e) {
			//System.out.println("hej1");

			int trainID = e.getTrainId();
			boolean hasStarted = trainList.get(trainID - 1).getHasStarted();
			int i = 0;

			if (e.getStatus() == 2 && !hasStarted) {
				trainList.get(trainID - 1).setHasStarted(true);
			} else if (e.getStatus() == 1){
				int speed = this.trainList.get(trainID - 1).getSpeed();
				stopTrain(trainID);
				waitALittle(trainID);
				startTrain(trainID, speed);
			}
		}

		void waitALittle(int tID) {
			int timeToWait = 1000 + (20 * Math.abs((trainList.get(tID - 1)).getSpeed()));
			try {				
				this.trainList.get(tID - 1).sleep(timeToWait);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		private void stopTrain(int tID) {
			System.out.println("stop train reached");
			this.trainList.get(tID - 1).brake();
		}

		private void startTrain(int tID, int oldSpeed) {
			this.trainList.get(tID - 1).setSpeed(oldSpeed * (-1));
		}
	}
}
