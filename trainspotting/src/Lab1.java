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

		Switch switch1 = new Switch(1, 17, 7, -1, 1 ,1, tsi, switchSem1, trainList, switchNeighbor1, terminalList1);
		Switch switch2 = new Switch(2, 15, 9, -1, 1 ,1, tsi, switchSem2, trainList, switchNeighbor2, null);
		Switch switch3 = new Switch(3, 4, 9, 1, 1 ,0, tsi, switchSem3, trainList, switchNeighbor3, null);
		Switch switch4 = new Switch(4, 3, 11, 0, 2 ,0, tsi, switchSem4, trainList, switchNeighbor4, terminalList2);

		ArrayList<Switch> switchList = new ArrayList<>(Arrays.asList(switch1, switch2, switch3, switch4));
		
		ArrayList<Switch> switch1NeighList = new ArrayList<>(Arrays.asList(switch2));
		ArrayList<Switch> switch2NeighList = new ArrayList<>(Arrays.asList(switch1, switch3));
		ArrayList<Switch> switch3NeighList = new ArrayList<>(Arrays.asList(switch2, switch4));
		ArrayList<Switch> switch4NeighList = new ArrayList<>(Arrays.asList(switch3));
		
		switch1.initSwitchNeigh(switch1NeighList);
		switch2.initSwitchNeigh(switch2NeighList);
		switch3.initSwitchNeigh(switch3NeighList);
		switch4.initSwitchNeigh(switch4NeighList);
		
		HashMap<Switch, Track> trainOnTrackMap1 = new HashMap<>();
		HashMap<Switch, Track> trainOnTrackMap2 = new HashMap<>();
		HashMap<Switch, Track> trainOnTrackMap3 = new HashMap<>();
		HashMap<Switch, Track> trainOnTrackMap4 = new HashMap<>();

		Track oneAndTwo = new Track();
		Track twoAndThree = new Track();
		Track threeAndFour = new Track();

		trainOnTrackMap1.put(switch2, oneAndTwo);

		trainOnTrackMap2.put(switch1, oneAndTwo);
		trainOnTrackMap2.put(switch3, twoAndThree);
		
		trainOnTrackMap3.put(switch2, twoAndThree);
		trainOnTrackMap3.put(switch4, threeAndFour);
		
		trainOnTrackMap4.put(switch3, threeAndFour);
		
		switch1.initTracks(trainOnTrackMap1);
		switch2.initTracks(trainOnTrackMap2);
		switch3.initTracks(trainOnTrackMap3);
		switch4.initTracks(trainOnTrackMap4);
		
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
	
	class Track {
		boolean hasTrain;
		
		public Track(){
			this.hasTrain = false;
		}

		public boolean getHasTrain(){
			return hasTrain;
		}

		public void setHasTrain(boolean newBool){
			this.hasTrain = newBool;
		}

	}

	class Train extends Thread {

		private volatile int speed;
		private int maxSpeed = 20; // TODO: Byt 
		private int id;
		volatile int sensorCounter = 0;
		Track nextTrack = null;
		
		TSimInterface tsi;
		private ArrayList<Switch> switchList;
		private ArrayList<Terminal> terminalList;
		//HashMap<Switch, Track> trainOnTrackMap = new HashMap<>();
		int semCounter = 0;
		int oldSpeed;

		boolean hasStarted = false;
		boolean isStopped = false;
		boolean justStarted = false; //temporär ksk?

		public Train(int id, int speed, TSimInterface tsi) {
			this.speed = speed;
			this.id = id;
			this.tsi = tsi;
			this.oldSpeed = this.speed;
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
				this.oldSpeed = this.speed;
				this.speed = newSpeed;
				this.tsi.setSpeed(this.id, speed);
			} catch (CommandException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public int getOldSpeed(){
			return this.oldSpeed;
		}

		public void brake() {
			setSpeed(0);
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

		public void setIsStopped(boolean newBool){
			this.isStopped = newBool;
		}

		public boolean getIsStopped(){
			return this.isStopped;
		}

		public void acquireSwitches(ArrayList<Switch> switchList) {
			this.switchList = switchList;
		}

		public void acquireTerminals(ArrayList<Terminal> terminalList) {
			this.terminalList = terminalList;
		}


		public boolean getJustStarted() {
			return justStarted;
		}

		public void setJustStarted(boolean justStarted) {
			this.justStarted = justStarted;
		}

		// public void updateTrackHashmap(HashMap<Switch, Track> switchTrackMap) {
		// 	this.trainOnTrackMap = switchTrackMap;
		// }
		
		public Track getNextTrack(){
			return this.nextTrack;
		}

		public void setNextTrack(Track nextTrack){
			this.nextTrack = nextTrack;
		}

		@Override
		public void run() {
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

		final int sXOffset;
		final int sYOffset;

		TSimInterface tsi;

		Sensor westSensor;
		Sensor eastSensor;
		Sensor southSensor;

		ArrayList<Sensor> sensors = new ArrayList<>();

		HashMap<int[], Sensor> sensorMap = new HashMap<>();
		HashMap<Switch, Track> trainOnTrackMap = new HashMap<>();

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
		Track trackToRelease = null;
		//Train stoppedTrain = null;
		
		//int oldSpeed;

		public Switch(int id, int posX, int posY, int sXOffset, int sYOffset, int ori, TSimInterface tsi, Semaphore switchSem,
				ArrayList<Train> trainList, ArrayList<Semaphore> switchNeighbor, 
				ArrayList<Terminal> terminalList) {
			this.id = id;
			this.posX = posX;
			this.posY = posY;
			this.sXOffset = sXOffset;
			this.sYOffset = sYOffset;
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

			westSensor = new Sensor(posX - 2, posY);
			eastSensor = new Sensor(posX + 2, posY);
			southSensor = new Sensor(posX + sXOffset, posY + sYOffset);

			this.sensors.add(westSensor);
			this.sensors.add(eastSensor);
			this.sensors.add(southSensor);

			sensorMap.put(this.westSensor.getPosList(), this.westSensor);
			sensorMap.put(this.eastSensor.getPosList(), this.eastSensor);
			sensorMap.put(this.southSensor.getPosList(), this.southSensor);
		}
		
		public HashMap<Switch, Track> getTrainOnTrackMap() {
			return this.trainOnTrackMap;
		}

		public void initTracks(HashMap<Switch, Track> trainsOnTrackMap){
			this.trainOnTrackMap = trainsOnTrackMap;
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

		private void stopAndRelease(int trainId) {//TODO: ta bort om den inte används
			this.switchSem.release();
			stopTrain(trainId);
		}

		private void stopTrain(int trainId) {
			System.out.println("stop train reached");
			this.trainList.get(trainId - 1).brake();
			//this.switchSem.release();
			this.trainList.get(trainId - 1).setIsStopped(true);
		}
		
		private void startTrain(int tID) {
			Train currentTrain = this.trainList.get(tID - 1);
			currentTrain.setSpeed(currentTrain.getOldSpeed());
			currentTrain.setIsStopped(false);
		}

		private void goSouth() {
			try {
				this.tsi.setSwitch(this.posX, this.posY, this.orientation);
			} catch (CommandException e) {
				System.out.println("rad 360 " + this.posX + " " + this.posY);
				e.printStackTrace();
			}
		}

		void resetTrackVal(){
			if(this.trackToRelease != null){
				this.trackToRelease.setHasTrain(false);
				for(Train train : trainList){
					if(train.getIsStopped()){
						System.out.println("train is stopped");
						Track trackToCheck = train.getNextTrack();
						if(trackToCheck != null && !trackToCheck.getHasTrain()){
						 	startTrain(train.getTrainId());
						 	train.setJustStarted(true);
						}
					}
				}
			}
		}

		String checkSensorPos(SensorEvent event){
			String val = "none";
			if (event.getXpos() == westSensor.posX && event.getYpos() == westSensor.posY) {
				return val = "w";
			} else if (event.getXpos() == eastSensor.posX && event.getYpos() == eastSensor.posY){
				return val = "e";
			} else if (event.getXpos() == southSensor.posX && event.getYpos() == southSensor.posY) {
				return val = "s";
			}
			return val;
		}

		void doStuffForCorrectSwitch(SensorEvent event){

			//this.trainList.get(event.getTrainId()-1).updateTrackHashmap(this.trainOnTrackMap);

			boolean hasTerminal = this.terminalList != null;
			int switchChangeDir = Math.abs(this.orientation - 1);
			boolean trainCanGoSouth = false;
			//Track nextTrack = null;
			
			switch(checkSensorPos(event)){
				case "w":
					this.trainList.get(event.getTrainId()-1).setNextTrack(this.trainOnTrackMap.get(switchNeighList.get(0)));  
					westSensor.changeStatus(event);
					if(!(hasTerminal)) {
						if(this.orientation == 0){
							trainCanGoSouth = true;	
						}
						System.out.println("ska kolla upcoming track");
						checkUpcomingTrack(event, 0, switchChangeDir, trainCanGoSouth);

						semaphoreToRelease[0] = this.switchSem;
						trackToRelease = this.trainOnTrackMap.get(switchNeighList.get(1)); // the previous track
					} else {
						semaphoreToRelease[0] = this.switchSem;
						if(this.orientation == 1){
							checkUpcomingTrack(event, 0, switchChangeDir, false);
							semaphoreToRelease[1] = this.terminalList.get(0).getTermSem();
						} else { // West -> East
							this.trainList.get(event.getTrainId()-1).setNextTrack(null);  
							trackToRelease = this.trainOnTrackMap.get(switchNeighList.get(0));
							acquireTerminal(1);
						}
					}
					break;
				case "e":
					eastSensor.changeStatus(event);
					if(!(hasTerminal)){
						this.trainList.get(event.getTrainId()-1).setNextTrack(this.trainOnTrackMap.get(switchNeighList.get(1)));
						if(this.orientation == 1){
							trainCanGoSouth = true;	
						}
						checkUpcomingTrack(event, 1, switchChangeDir, trainCanGoSouth);
						semaphoreToRelease[0] = this.switchSem;
						trackToRelease = this.trainOnTrackMap.get(switchNeighList.get(0)); // the previous track
					} else {
						semaphoreToRelease[0] = this.switchSem;
						
						if(this.orientation == 1){
							this.trainList.get(event.getTrainId()-1).setNextTrack(null);
							trackToRelease = this.trainOnTrackMap.get(switchNeighList.get(0));
							acquireTerminal(0);
						} else {// West -> East
							this.trainList.get(event.getTrainId()-1).setNextTrack(this.trainOnTrackMap.get(switchNeighList.get(0)));
							checkUpcomingTrack(event, 0, switchChangeDir, false);
							semaphoreToRelease[1] = this.terminalList.get(0).getTermSem();
						}
					}
					break;
				case "s":
					southSensor.changeStatus(event);
					if(!(hasTerminal)){
						this.trainList.get(event.getTrainId()-1).setNextTrack(this.trainOnTrackMap.get(switchNeighList.get(switchChangeDir)));
						checkUpcomingTrack(event, switchChangeDir, this.orientation, false); //switchChangeDir och orientation är "motsatser"
						semaphoreToRelease[0] = this.switchSem;
					} else {
						int nextSwitch = 0; 
						this.trainList.get(event.getTrainId()-1).setNextTrack(this.trainOnTrackMap.get(switchNeighList.get(nextSwitch)));
						checkUpcomingTrack(event, nextSwitch, this.orientation, false);
						semaphoreToRelease[0] = this.switchSem;
						semaphoreToRelease[1] = this.terminalList.get(1).getTermSem();
					}
					break;
				default:
					break;
			}
		}

		void acquireTerminal(int switchDirStraight){
			try{
				if (terminalList.get(0).getTermSem().tryAcquire()) {
					System.out.println("acquired the first terminal");
					this.tsi.setSwitch(posX, posY, switchDirStraight);
				} else if(terminalList.get(1).getTermSem().tryAcquire()) {
					System.out.println("the first terminal was occupied");
					goSouth();
				}	
			} catch (CommandException e) {
				e.printStackTrace();
			}
		
		}

		void checkUpcomingTrack(SensorEvent event, int nextSwitch, int switchChangeDirection, boolean canGoSouth){
			if (!(this.trainOnTrackMap.get(switchNeighList.get(nextSwitch)).getHasTrain())){
				this.trainOnTrackMap.get(switchNeighList.get(nextSwitch)).setHasTrain(true);
				System.out.println("made it to 504");
				changeSwitchIfSafe(event, switchChangeDirection);
			} else if (canGoSouth){
				goSouth();
			} else {
				System.out.println("upcoming track was taken");
				stopTrain(event.getTrainId());
			}	
		}
		
		

		void changeSwitchIfSafe(SensorEvent event, int switchDir){
			if (this.switchSem.tryAcquire()) { //bytte till nuvarande
			
				System.out.println("Acquired semaphore " + this.switchSem);
				try {
					System.out.println("Switch flips");
					this.tsi.setSwitch(this.posX, this.posY, switchDir);
				} catch (CommandException e) {
					e.printStackTrace();
				}

			} else {
				System.out.println("couldn't acquire semaphore " + this.switchSem);
				stopTrain(event.getTrainId());
			}
		}
			

		void changeSwitch(SensorEvent e) {

			if (this.sensorCounter < 2 || this.trainList.get(e.getTrainId() -1).getJustStarted()) {
				this.trainList.get(e.getTrainId() -1).setJustStarted(false);
				doStuffForCorrectSwitch(e);
			}

			newCheckSensorCounter(semaphoreToRelease);
			System.out.println("sensorCounter " + sensorCounter);
		}

		public void newCheckSensorCounter(Semaphore[] semsToRelease) {
			if (sensorCounter == 4) {	
				this.switchSem.release();	
				for (Semaphore sem : semsToRelease) {
					if (sem != null) {
						sem.release();
						System.out.println(sem + " checking sems row 576");	
					}
				}
				resetTrackVal();
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

			int trainID = e.getTrainId();
			boolean hasStarted = trainList.get(trainID - 1).getHasStarted();

			if (e.getStatus() == 2 && !hasStarted) {
				System.out.println("how many times?");
				terminalSem.tryAcquire();
				trainList.get(trainID - 1).setHasStarted(true);
			} else if (e.getStatus() == 1){
				int speed = this.trainList.get(trainID - 1).getSpeed();
				System.out.println("terminal sensor event");
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
				e1.printStackTrace();
			}
		}

		private void stopTrain(int tID) {
			System.out.println("terminal stop");
			this.trainList.get(tID - 1).brake();
		}

		private void startTrain(int tID, int oldSpeed) {
			this.trainList.get(tID - 1).setSpeed(oldSpeed * (-1));
		}
	}
}
