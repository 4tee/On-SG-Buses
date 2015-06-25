package net.felixmyanmar.onsgbuses.container;

/**
 * Created by rhymes_mcpro on 20/5/15.
 */
public class BusRoute {

    String service_id;
    int direction;
    int sequence;
    int route_segment_id;

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getRoute_segment_id() {
        return route_segment_id;
    }

    public void setRoute_segment_id(int route_segment_id) {
        this.route_segment_id = route_segment_id;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public String getService_id() {
        return service_id;
    }

    public void setService_id(String service_id) {
        this.service_id = service_id;
    }
}
