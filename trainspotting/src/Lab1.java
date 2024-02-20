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

		/*
		 * The declarations of the trains, switches, terminals, intersection, and semaphores.
		 */
		TSimInterface tsi = TSimInterface.getInstance();

		Train train1 = new Train(1, speed1, 1, tsi);
		Train train2 = new Train(2, speed2, -1, tsi);

		ArrayList<Train> trainList = new ArrayList<Train>();
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

		Semaphore intersectSem = new Semaphore(1);

		ArrayList<Semaphore> switchNeighbor1 = new ArrayList<>(Arrays.asList(switchSem2));
		ArrayList<Semaphore> switchNeighbor2 = new ArrayList<>(Arrays.asList(switchSem1, switchSem3));
		ArrayList<Semaphore> switchNeighbor3 = new ArrayList<>(Arrays.asList(switchSem2, switchSem4));
		ArrayList<Semaphore> switchNeighbor4 = new ArrayList<>(Arrays.asList(switchSem3));

		Terminal terminal1 = new Terminal(tsi, 16, 3, startSem1, trainList, switchSem1);
		Terminal terminal2 = new Terminal(tsi, 16, 5, startSem2, trainList, switchSem1);
		Terminal terminal3 = new Terminal(tsi, 16, 11, startSem3, trainList, switchSem4);
		Terminal terminal4 = new Terminal(tsi, 16, 13, startSem4, trainList, switchSem4);

		Intersection intersection = new Intersection(tsi, 8, 7, intersectSem, trainList);

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

		Track oneAndTwo = new Track(0);
		Track twoAndThree = new Track(1);
		Track threeAndFour = new Track(2);

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
		
		for(Train train : trainList){
			train.acquireSwitches(switchList);
			train.acquireTerminals(terminalList3);
			train.acquireIntersection(intersection);
		}
		
		/*
		 * Start the trains 
		 */
		train1.start();
		train2.start();

		try {
			tsi.setSpeed(train1.getTrainId(), train1.getSpeed());
			tsi.setSpeed(train2.getTrainId(), train2.getSpeed());

		} catch (CommandException e) {
			e.printStackTrace(); // or only e.getMessage() for the error
			System.exit(1);
		}
		
		
	}
	
	/*
	 * Track class.
	 */
	class Track {

		boolean hasTrain;
		int trackId;
		
		public Track(int id){
			this.hasTrain = false;
			this.trackId = id;
		}

		public boolean getHasTrain(){
			return hasTrain;
		}

		public void setHasTrain(boolean newBool){
			this.hasTrain = newBool;
		}

	}

	/*
	 * Train class.
	 * This class also contains the run-method that the threads use.
	 */
	class Train extends Thread {

		private volatile int speed;
		private int id;
		private int startDir; // Train 1: 1, Train 2: -1
		volatile int sensorCounter = 0; //used to measure how many of the sensors of a switch or intersection the train has passed. 
		// the sensorCounter is incremented once every time the train activates or inactivates a sensor so it will become 4 once it has passed a switch or intersection.
		Track nextTrack = null;
		Track currentTrack = null;
		SensorEvent lastEvent = null;
		int terminalToRelease = -1;
		
		TSimInterface tsi;
		private ArrayList<Switch> switchList;
		private ArrayList<Terminal> terminalList;
		private Intersection intersection;
		int semCounter = 0;
		int oldSpeed;

		boolean hasStarted = false;
		boolean isStopped = false;
		boolean justStarted = false;
		boolean hasTerminal = false;
		boolean headingFromTerminal = false;

		public Train(int id, int speed, int startDir, TSimInterface tsi) {
			this.speed = speed;
			this.id = id;
			this.startDir = startDir;
			this.tsi = tsi;
			this.oldSpeed = this.speed;
		}

		public boolean getHeadingFromTerminal() {
			return this.headingFromTerminal;
		}

		public void setHeadingFromTerminal(boolean headingFromTerminal) {
			this.headingFromTerminal = headingFromTerminal;
		}

		public void incSensorCounter() {
			sensorCounter++;
		}
		
		private void resetSensorCounter() {
			sensorCounter = 0;
		}

		/*
		 * If the sensorCounter is 4, the train has passed the switch or intersection so the counter is reset.
		 */
		private void checkSensorCounter() {
			if(sensorCounter == 4)
				resetSensorCounter();
		}

		public int getSensorCounter() {
			return this.sensorCounter;
		}
		
		public int getSpeed() {
			return speed;
		}
		
		/*
		 * Method to set the speed of the train, this method also saves the previous speed.
		 */
		public void setSpeed(int newSpeed) {
			try {
				this.oldSpeed = this.speed;
				this.speed = newSpeed;
				this.tsi.setSpeed(this.id, speed);
			} catch (CommandException e) {
				e.printStackTrace();
			}
		}
		
		public int getOldSpeed(){
			return this.oldSpeed;
		}

		public void brake() {
			setSpeed(0);
		}

		public void acquireSwitches(ArrayList<Switch> switchList) {
			this.switchList = switchList;
		}

		public void acquireTerminals(ArrayList<Terminal> terminalList) {
			this.terminalList = terminalList;
		}

		public void acquireIntersection(Intersection intersection){
			this.intersection = intersection;
		}

		public int getTrainId() {
			return id;
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

		public boolean getJustStarted() {
			return justStarted;
		}

		public void setJustStarted(boolean justStarted) {
			this.justStarted = justStarted;
		}
		
		public int getStartDir(){
			return this.startDir;
		}

		public boolean getHasTerminal() {
			return this.hasTerminal;
		}

		public void setHasTerminal(boolean newBool) {
			this.hasTerminal = newBool;
		}

		public Track getCurrentTrack(){
			return this.currentTrack;
		}

		public void setCurrentTrack(Track aTrack){
			this.currentTrack = aTrack;
		}
		
		public Track getNextTrack(){
			return this.nextTrack;
		}

		public void setNextTrack(Track nextTrack){
			this.nextTrack = nextTrack;
		}
		
		public void setLastEvent(SensorEvent e){
			this.lastEvent = e;
		}

		public SensorEvent getLastEvent() {
			return this.lastEvent;
		}

		public int getTerminalNum(){
			return this.terminalToRelease;
		}

		public void setTerminalNum(int termToRelease){
			this.terminalToRelease = termToRelease;
		}

		/*
		 * Method to check if an event that occured is related to a switch and returns that switch if so.
		 */
		Switch checkIfEventInSwitch(SensorEvent event){
			int ePos[] = new int[] { event.getXpos(), event.getYpos() };
			for (int i = 0; i < 4; i++) {
				Set<int[]> keySet = switchList.get(i).getSensorMap().keySet();
				for (int[] key : keySet) {
					if (key[0] == ePos[0] && key[1] == ePos[1]) {
						Switch correctSwitch = switchList.get(i);
						return correctSwitch;
					}
				}
			}
			return null;
		}
	
		/*
		 * Main run method, any sensor event that occurs goes through this method.
		 */
		@Override
		public void run() {
			while (true) {
				SensorEvent event;
				try {
					event = tsi.getSensor(this.id);
					int ePos[] = new int[] { event.getXpos(), event.getYpos() };

					for (int i = 0; i < 4; i++) {
						Set<int[]> keySet = switchList.get(i).getSensorMap().keySet();
						for (int[] key : keySet) {
							if (key[0] == ePos[0] && key[1] == ePos[1]) {
								this.incSensorCounter();
								switchList.get(i).onSensorEvent(event);
								this.checkSensorCounter();
							}
						}
					}

					for (Terminal terminal : terminalList) {
						if (terminal.getPos()[0] == ePos[0] && terminal.getPos()[1] == ePos[1])
							terminal.onTerminalSensorEvent(event);
					}

					//InterSection
					intersection.onIntersectionSensorEvent(event);
					this.checkSensorCounter();

				} catch (CommandException | InterruptedException e) {
					e.printStackTrace();
				}

			}
		}

		/*
		 * Method to start the train object if stopped
		 */
		public void startIfStopped() {
			Switch switchToChange = checkIfEventInSwitch(this.lastEvent);
			if(switchToChange != null){
				this.setIsStopped(false);
				this.setJustStarted(true);
				switchToChange.onSensorEvent(this.lastEvent);
				switchToChange.startTrain(this.id-1);
			} else {
				intersection.startTrain(this.lastEvent);
			}
		}
	}

	/*
	 * Swich Class.
	 * Contains most of the important logic.
	 */
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

		int orientation; //0 or 1
		ArrayList<Train> trainList;
		ArrayList<Semaphore> switchNeighSemList;
		ArrayList<Switch> switchNeighList;
		ArrayList<Terminal> terminalList;
		int neighborAmount;
		Semaphore terminalSemToRelease = null;
		Track trackToRelease = null;

		/*
		 * Switch constructor
		 */
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

		/*
		 * Method to initialize the switch's internal sensors.
		 * The sensors' positions are hard coded.
		 */
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

		void onSensorEvent(SensorEvent e) {
			acquireSwitchOrStop(e);
		}

		/*
		 * Method to stop a train and release the appropriate semaphore
		 */
		private void releaseAndStop(int trainId) {
			stopTrain(trainId);
			this.switchSem.release();
		}

		/*
		 * Method to stop a train
		 */
		private void stopTrain(int trainId) {
			this.trainList.get(trainId).brake();
			this.trainList.get(trainId).setIsStopped(true);
		}
		
		/*
		 * Method to start a train
		 */
		private void startTrain(int tID) {
			Train currentTrain = this.trainList.get(tID);
			currentTrain.setSpeed(currentTrain.getOldSpeed());
			currentTrain.setIsStopped(false);
		}

		/*
		 * Method to tell the switch to change southwards.
		 */
		private void goSouth(SensorEvent event) {
			changeSwitchIfSafe(event, this.orientation);
		}

		/*
		 * Method to determine if any train has stopped at a switch, and if so, start them.
		 * This method should only be called upon when you're sure that no collisions or
		 * derailment errors can occur.
		 */
		void startStoppedTrain(){
			for(Train train : trainList){
				if(train.getIsStopped()){
					Track trackToCheck = train.getNextTrack();
					if(trackToCheck != null && !trackToCheck.getHasTrain()){
						//int trainId = train.getTrainId() - 1; TODO: ta bort
						train.startIfStopped();
					}
				}
			}
		}

		/*
		 * Method to get the correct switch for an event
		 */
		Switch getSwitch(SensorEvent event, Train train) {
			Switch correctSwitch = train.checkIfEventInSwitch(event);
			return correctSwitch;
		}

		/*
		 * Method to check if an event corresponds to any of a switch's sensors.
		 * Will return "none" if false.
		 */
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

		/**
		 * The master method for switches, whenever a train reaches a switch their initial behaviour is determined here.
		 */
		void doStuffForCorrectSwitch(SensorEvent event){
			
			boolean hasTerminal = this.terminalList != null;
			int switchChangeDir = Math.abs(this.orientation - 1);
			boolean trainCanGoSouth = false;
			int innerTrainId = event.getTrainId() -1;
			Train train = this.trainList.get(innerTrainId);

			/*
			 * Switch statement to check the event's position and if it corresponds correctly with a sensor in a switch
			 */
			switch(checkSensorPos(event)){
				/*
				 * Case: The event corresponded to some switch's western sensor
				 */
				case "w":
					train.setNextTrack(this.trainOnTrackMap.get(switchNeighList.get(0)));  
					if(!(hasTerminal)) {
						if(this.orientation == 0){
							trainCanGoSouth = true;	
						}
						checkUpcomingTrack(event, 0, switchChangeDir, trainCanGoSouth);
					} else {

						if(this.orientation == 1){
							checkUpcomingTrack(event, 0, switchChangeDir, false);
							train.setTerminalNum(0); 
						} else { // West -> East
							acquireTerminal(1, event);
						}
					}
					break;
				/*
				 * Case: The event corresponded to some switch's eastern sensor
				 */
				case "e":
					if(!(hasTerminal)){
						train.setNextTrack(this.trainOnTrackMap.get(switchNeighList.get(1)));
						if(this.orientation == 1){
							trainCanGoSouth = true;	
						}
						checkUpcomingTrack(event, 1, switchChangeDir, trainCanGoSouth);
					} else {
						
						if(this.orientation == 1){
							acquireTerminal(0, event);
						} else {// West -> East
							train.setNextTrack(this.trainOnTrackMap.get(switchNeighList.get(0)));
							checkUpcomingTrack(event, 0, switchChangeDir, false);
							train.setTerminalNum(0);
						}
					}
					break;
				/*
				 * Case: The event corresponded to some switch's southern sensor
				 */
				case "s":
					train.setCurrentTrack(null);
					if(!(hasTerminal)){
						train.setNextTrack(this.trainOnTrackMap.get(switchNeighList.get(switchChangeDir)));
						checkUpcomingTrack(event, switchChangeDir, this.orientation, false); //switchChangeDir och orientation är "motsatser"
					} else {
						int nextSwitch = 0; 
						train.setNextTrack(this.trainOnTrackMap.get(switchNeighList.get(nextSwitch)));
						checkUpcomingTrack(event, nextSwitch, this.orientation, false);
					}
					break;
				default:
					break;
			}
		}
		
		/*
		 * Method to acquire a free terminal, the method will always try and acquire the top-most
		 * terminal of each pair first.
		 */
		void acquireTerminal(int switchDirStraight, SensorEvent event){
			Train train = this.trainList.get(event.getTrainId() - 1);
			train.setHeadingFromTerminal(false);
			if (this.terminalList.get(0).getTermSem().tryAcquire()) {
				changeSwitchIfSafe(event, switchDirStraight);
			} else if(this.terminalList.get(1).getTermSem().tryAcquire()) {
				train.setTerminalNum(1);
				goSouth(event);
			}
		}

		/*
		 * Method to check if the next track has a train on it, and if it does, call the appropriate 
		 * method to handle the situation.
		 */
		void checkUpcomingTrack(SensorEvent event, int nextSwitch, int switchChangeDirection, boolean canGoSouth){
			int theTrainId = event.getTrainId()-1;
			if (!(this.trainOnTrackMap.get(switchNeighList.get(nextSwitch)).getHasTrain())){
				this.trainOnTrackMap.get(switchNeighList.get(nextSwitch)).setHasTrain(true);
				changeSwitchIfSafe(event, switchChangeDirection);
			} else if (canGoSouth){
				goSouth(event);
			} else {
				releaseAndStop(theTrainId);
			}	
		}
		
		/*
		 * Method to designate the different tracks correctly between the trains.
		 */
		void setNextTrackForTrain(int trainId) {
			Train train = trainList.get(trainId);
			int nextSwitch = 0;
			int prevSwitch = 1;
			
			if(switchNeighList.size() == 1 && train.getHasTerminal() && !(train.getHeadingFromTerminal())){
				train.setNextTrack(this.trainOnTrackMap.get(switchNeighList.get(nextSwitch)));
			} else {
				int nextSwitchDir = (train.getStartDir() * train.getSpeed());
				if(nextSwitchDir > 0){
					nextSwitch = 1;
					prevSwitch = 0;
				}
				if(switchNeighList.size() > 1){
					train.setCurrentTrack(this.trainOnTrackMap.get(switchNeighList.get(prevSwitch)));
					train.setNextTrack(this.trainOnTrackMap.get(switchNeighList.get(nextSwitch)));
				} else {
					train.setNextTrack(this.trainOnTrackMap.get(switchNeighList.get(0)));
				}
			}
		}

		/*
		 * Method to change the switch direction if safe, this method should only
		 * be called upon when you're sure that no issues will occur when changing the switch.
		 */
		void changeSwitchIfSafe(SensorEvent event, int switchDir){ 
			try {
				this.tsi.setSwitch(this.posX, this.posY, switchDir);
			} catch (CommandException e) {
				e.printStackTrace();
			}
		}
		
		/*
		 * Method to check if a switch is free, or if the train needs to stop.
		 */
		void acquireSwitchOrStop(SensorEvent e) {

			int trainId = e.getTrainId() - 1;
			Train train = trainList.get(trainId);
			train.setLastEvent(e);

			/*
			 * Check if the train's internal sensorCounter < 2, or if it just has started i.e. if it 
			 * needed to stop at the switch.
			 */
			if (trainList.get(trainId).sensorCounter < 2 || this.trainList.get(trainId).getJustStarted()) {
				
				if (this.switchSem.tryAcquire()){
					if(!(train.getJustStarted())){
						train.setCurrentTrack(train.getNextTrack());
					} 
					this.trainList.get(trainId).setJustStarted(false);
					doStuffForCorrectSwitch(e);
				} else {
					this.trainList.get(trainId).setJustStarted(false);
					setNextTrackForTrain(trainId);
					stopTrain(trainId);
				}
			}
			checkSensorCounter(trainId);
		}

		/* 
		* This method resets the correct semaphores when appropriate
		*/
		public void checkSensorCounter(int trainId) {
			Train train = trainList.get(trainId);
			
			/*
			 * Initial check if train has passed switch or intersection
			 */
			if (train.getSensorCounter() == 4) {
				int terminalToRelease = train.getTerminalNum();

				/*
				 * Check if there is a terminal semaphore to release and the train is leaving a terminal.
				 * If this isn't the case, reset the track val instead.
				 */
				if (terminalToRelease >= 0 && train.headingFromTerminal){
					terminalList.get(terminalToRelease).getTermSem().release();
					train.setHasTerminal(false);
					train.setTerminalNum(-1);
					train.setHeadingFromTerminal(true);
				} else {
					resetTrackVal(trainId);
				}

				/*
				 * General check if the train is not heading directly from a terminal,
				 * if headingFromTermnial is false, then reset track val.
				 */
				if (!(train.getHeadingFromTerminal())){
					resetTrackVal(trainId);
				}
				train.setHeadingFromTerminal(false);
				this.switchSem.release();
				startStoppedTrain();	
			}
		}

		void resetTrackVal(int trainId) {
			Train train = trainList.get(trainId);
			this.trackToRelease = train.getCurrentTrack();
			if(this.trackToRelease != null){
				this.trackToRelease.setHasTrain(false);
			}
			if(train.getHasTerminal()){
				train.setCurrentTrack(null);
			}
		}
	}

	/*
	 * Main sensor class
	 */
	class Sensor {

		final int posX;
		final int posY;
		SensorEvent latestEvent;

		/*
		 * Sensor constructor
		 */
		public Sensor(int posX, int posY) {
			this.posX = posX;
			this.posY = posY;
		}

		/*
		 * Method to get the sensor's position as a list with two elements.
		 */
		public int[] getPosList() {
			int temp[] = new int[] { this.posX, this.posY };

			return temp;
		}
		
		/*
		 * Method to get the last event logged at a sensor. 
		 * If the event status is 2, then the sensor is inactive, if it is 1, then it is active.
		 */
		public SensorEvent getLatestEvent() {
			return latestEvent;
		}
	}

	/*
	 * Main terminal class
	 */
	class Terminal {

		TSimInterface tsi;
		Sensor sensor;

		Semaphore terminalSem;
		Semaphore neighSem;

		ArrayList<Train> trainList;

		/*
		 * Terminal constructor
		 */
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

		/*
		 * Method to initialize the terminal's sensor correctly
		 */
		void initSensor(int posX, int posY) {
			this.sensor = new Sensor(posX, posY);
		}

		/*
		 * Method to handle SensorEvents at the terminal. 
		 */
		void onTerminalSensorEvent(SensorEvent e) {

			int trainID = e.getTrainId() - 1;
			Train train = trainList.get(trainID);
			boolean hasStarted = trainList.get(trainID).getHasStarted();

			/*
			 * Main terminal logic block.
			 * 
			 * First if-statement handles the train's first start from the terminal.
			 * 
			 * The else-if handles the arriving train's behaviour at the terminal, such as stopping, waiting,
			 * and starting again.
			 */
			if (e.getStatus() == 2 && !hasStarted) {
				if(terminalSem.tryAcquire()){
					train.setTerminalNum(0);
					trainList.get(trainID).setHasStarted(true);
				}
			} else if (e.getStatus() == 1){
				int speed = this.trainList.get(trainID).getSpeed();
				startStoppedTrain();
				stopTrain(trainID);
				waitALittle(trainID);
				startTrain(trainID, speed);
			}
			
			this.trainList.get(trainID).setHeadingFromTerminal(true);
			this.trainList.get(trainID).setHasTerminal(true);
		}

		/*
		 * Method to determine how long a train should wait at the terminal
		 */
		void waitALittle(int tID) {
			int timeToWait = 1000 + (20 * Math.abs((trainList.get(tID)).getSpeed()));
			try {				
				this.trainList.get(tID).sleep(timeToWait);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}

		/*
		 * Method to stop the train arriving at the terminal
		 */
		private void stopTrain(int tID) {
			this.trainList.get(tID).brake();
		}

		/*
		 * Method to start the train at the terminal
		 */
		private void startTrain(int tID, int oldSpeed) {
			this.trainList.get(tID).setSpeed(oldSpeed * (-1));
		}

		/*
		 * Method to prevent trains from unnecessarily waiting at switches when the other is at a terminal
		 */
		private void startStoppedTrain(){
			for(Train train : trainList){
				if(train.getIsStopped()){
					int trainId = train.getTrainId() - 1;
					train.startIfStopped();
				}
			}
		}
	}

	/*
	 * Main Intersection Class
	 */
	class Intersection {
		TSimInterface tsi;

		ArrayList<Sensor> sensors = new ArrayList<Sensor>();
		ArrayList<Train> trainList = new ArrayList<Train>();

		Sensor northSensor;
		Sensor eastSensor;
		Sensor southSensor;
		Sensor westSensor;

		Semaphore intersectSem;

		/*
		 * Intersection constructor
		 */
		public Intersection(TSimInterface tsi, int posX, int posY, Semaphore intersectSem, ArrayList<Train> trainList) {

			this.tsi = tsi;
			this.intersectSem = intersectSem;
			this.trainList = trainList;
			
			initSensor(posX, posY);
		}

		/*
		 * Method to initialize the intersection's sensors correctly
		 */
		void initSensor(int posX, int posY) {
			northSensor = new Sensor(posX, posY - 2);
			westSensor = new Sensor(posX - 2, posY);
			eastSensor = new Sensor(posX + 2, posY);
			southSensor = new Sensor(posX + 1, posY + 1);

			this.sensors.add(northSensor);
			this.sensors.add(westSensor);
			this.sensors.add(eastSensor);
			this.sensors.add(southSensor);
		}
		
		public Semaphore getIntersectSem(){
			return this.intersectSem;
		}

		/*
		 * Method to handle intersection events
		 */
		public void onIntersectionSensorEvent(SensorEvent e){
			Train train = trainList.get(e.getTrainId()-1);
			train.setLastEvent(e);
			
			if(eventConnectedToIntersection(e)){
				if(!(train.getJustStarted())){
					train.incSensorCounter();
				}
				acquireInterOrBrake(train);
			}
		}

		/*
		 * Method to attempt to acquire the intersection semaphore if available, otherwise stop the train
		 */
		void acquireInterOrBrake(Train train){
			/*
			 * This if-statement checks whether or not the relevant train's internal counter is
			 * < 2 or if the train has just started. If either is true, then the train should try and
			 * acquire the intersection, if this fails the train should stop. 
			 */
			if (train.sensorCounter < 2 || train.getJustStarted()) {
				/* If one can acquire this semaphore then they should just continue as normal and
					thus, nothing actually happens here. But the intersection should still be acquired
					to prevent the other train from passing through the intersection and colliding with
					the currently passing train. */
				if(this.intersectSem.tryAcquire()){
				} else {
					train.setIsStopped(true);
					train.brake();
				}
			}
			checkSensorCounter(train);
		}

		/*
		 * Method to check if an event is relevant to the intersection or not.
		 */
		private boolean eventConnectedToIntersection(SensorEvent event){
			for(Sensor sensor : sensors){
				if((event.getXpos() == sensor.getPosList()[0]) && event.getYpos() == sensor.getPosList()[1] ){
					return true;
				}
			}
			return false;
		}

		/*
		 * Method to check the train's internal sensor counter. 
		 * If said counter has the value 4, then release the intersection and start any train that is stopped
		 * as long as it is not stopped at a terminal.
		 */
		private void checkSensorCounter(Train train) {
			if (train.getSensorCounter() == 4) {
				this.intersectSem.release();
				startStoppedTrain();
			}
		}

		/*
		 * Method to check if any train is stopped anywhere but a terminal, and if so, start them again.
		 */
		private void startStoppedTrain(){
			for(Train train : trainList){
				if(train.getIsStopped()){
					train.startIfStopped();
				}
			}
		}

		/*
		 * Method to start a train specifically if the event is connected to the intersection.
		 */
		private void startTrain(SensorEvent event) {
			if(eventConnectedToIntersection(event)){
				Train currentTrain = this.trainList.get(event.getTrainId()-1);
				currentTrain.setSpeed(currentTrain.getOldSpeed());
				currentTrain.setIsStopped(false);
			}
		}
	}

}
