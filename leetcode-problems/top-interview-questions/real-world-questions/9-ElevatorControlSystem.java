import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.TreeSet;

enum Direction {
    UP,
    DOWN,
    NONE
}

enum ElevatorState {
    IDLE,
    MOVING,
    DOOR_OPEN
}

class Elevator {

    int id;
    int currentFloor = 0;
    Direction direction = Direction.NONE;
    ElevatorState state = ElevatorState.IDLE;
    TreeSet<Integer> upRequests = new TreeSet<>();
    TreeSet<Integer> downRequests = new TreeSet<>();

    public Elevator(int id) {
        this.id = id;
    }

    public void addExternalRequest(int requestedFloor, Direction requestedDirection) {
        if (requestedDirection == Direction.UP) {
            this.upRequests.add(requestedFloor);
            return;
        }
        if (requestedDirection == Direction.DOWN) {
            this.downRequests.add(requestedFloor);
            return;
        }
    }

    public void addInternalRequest(int requestedFloor) {
        if (requestedFloor > this.currentFloor) {
            this.upRequests.add(requestedFloor);
        } else if (requestedFloor < currentFloor) {
            this.downRequests.add(requestedFloor);
        } else {
            this.stopAt(this.currentFloor);
        }
    }

    private void stopAt(int floor) {
        System.out.println("Elevator " + this.id + " stopped at floor " + floor);
        state = ElevatorState.DOOR_OPEN;
        state = ElevatorState.MOVING;
    }

    public void step() {
        // move one step, process stop if needed
        if (this.isIdle()) {
            this.nextStepWhenIdle();
        }
        if (this.isGoingDown()) {
            this.nextStepWhenGoingDown();
            return;
        }
        if (this.isGoingUp()) {
            this.nextStepWhenGoingUp();
            return;
        }
    }

    private boolean isIdle() {
        return state == ElevatorState.IDLE;
    }

    private boolean isGoingDown() {
        return direction == Direction.DOWN;
    }

    private boolean isGoingUp() {
        return direction == Direction.UP;
    }

    private void nextStepWhenIdle() {
        if (!this.upRequests.isEmpty()) {
            this.setStatusGoingUp();
        } else if (!this.downRequests.isEmpty()) {
            this.setStatusGoingDown();
        } else {
            this.setStatusIdle();
        }
    }

    private void setStatusIdle() {
        direction = Direction.NONE;
        state = ElevatorState.IDLE;
    }

    private void setStatusGoingDown() {
        direction = Direction.DOWN;
        state = ElevatorState.MOVING;
    }

    private void setStatusGoingUp() {
        direction = Direction.UP;
        state = ElevatorState.MOVING;
    }

    private void nextStepWhenGoingDown() {
        this.currentFloor--;
        if (this.downRequests.contains(currentFloor)) {
            this.stopAt(this.currentFloor);
            this.downRequests.remove(this.currentFloor);
        }
        if (this.downRequests.isEmpty()) {
            if (!this.upRequests.isEmpty()) {
                this.setStatusGoingUp();
            } else {
                this.setStatusIdle();
            }
        }
    }

    private void nextStepWhenGoingUp() {
        this.currentFloor++;
        if (this.upRequests.contains(currentFloor)) {
            this.stopAt(this.currentFloor);
            this.upRequests.remove(this.currentFloor);
        }
        if (this.upRequests.isEmpty()) {
            if (!this.downRequests.isEmpty()) {
                this.setStatusGoingDown();
            } else {
                this.setStatusIdle();
            }
        }
    }

}

class ExternalRequest {

    int floor;
    Direction direction;

    public ExternalRequest(int floor, Direction direction) {
        this.floor = floor;
        this.direction = direction;
    }

}

class InternalRequest {

    int elevatorId;
    int destinationFloor;

    public InternalRequest(int elevatorId, int destinationFloor) {
        this.elevatorId = elevatorId;
        this.destinationFloor = destinationFloor;
    }

}

class ElevatorSchedular {

    List<Elevator> elevators;
    Queue<ExternalRequest> pendingExternalRequests = new LinkedList<>();

    public ElevatorSchedular(int numberOfElevators) {
        this.elevators = new ArrayList<>();
        for (int i = 0; i < numberOfElevators; i++) {
            this.elevators.add(new Elevator(i));
        }
    }

    public void handleExternalRequest(ExternalRequest request) {
        Optional<Elevator> bestElevator = this.findBestElevator(request);
        if (bestElevator.isPresent()) {
            bestElevator.get().addExternalRequest(request.floor, request.direction);
        } else {
            this.pendingExternalRequests.add(request);
        }
    }

    public void handleInternalRequest(InternalRequest request) {
        if (request.elevatorId >= 0 && request.elevatorId < this.elevators.size()) {
            this.elevators.get(request.elevatorId)
                    .addInternalRequest(request.destinationFloor);
        }
    }

    public void stepAll() {
        for (Elevator elevator : this.elevators) {
            elevator.step();
        }

        Queue<ExternalRequest> retry = new LinkedList<>();
        while (!this.pendingExternalRequests.isEmpty()) {
            ExternalRequest request = this.pendingExternalRequests.poll();
            if (!this.assign(request)) {
                retry.add(request);
            }
        }
        this.pendingExternalRequests = retry;
    }

    private boolean assign(ExternalRequest request) {
        Optional<Elevator> elevator = this.findBestElevator(request);
        if (elevator.isPresent()) {
            elevator.get().addExternalRequest(request.floor, request.direction);
            return true;
        }
        return false;
    }

    private Optional<Elevator> findBestElevator(ExternalRequest request) {
        Optional<Elevator> bestElevator = Optional.empty();
        int minDistance = Integer.MAX_VALUE;

        for (Elevator elevator : this.elevators) {
            if (this.canServe(elevator, request)) {
                int dist = Math.abs(elevator.currentFloor - request.floor);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestElevator = Optional.of(elevator);
                }
            }
        }

        return bestElevator;
    }

    private boolean canServe(Elevator elevator, ExternalRequest request) {
        if (elevator.state == ElevatorState.IDLE) {
            return true;
        }
        if (elevator.direction == request.direction) {
            if (request.direction == Direction.UP && elevator.currentFloor <= request.floor) {
                return true;
            }
            if (request.direction == Direction.DOWN && elevator.currentFloor >= request.floor) {
                return true;
            }
        }

        return false;
    }

}

class ElevatorControlSystem {

    public static void main(String[] args) {
        ElevatorSchedular scheduler = new ElevatorSchedular(2);

        scheduler.handleExternalRequest(new ExternalRequest(3, Direction.UP));
        scheduler.handleExternalRequest(new ExternalRequest(5, Direction.UP));
        scheduler.handleExternalRequest(new ExternalRequest(2, Direction.DOWN));

        scheduler.handleInternalRequest(new InternalRequest(0, 6));
        scheduler.handleInternalRequest(new InternalRequest(1, 1));

        for (int i = 0; i < 10; i++) {
            System.out.println("--- Time Step " + i + " ---");
            scheduler.stepAll();
        }
    }

}
