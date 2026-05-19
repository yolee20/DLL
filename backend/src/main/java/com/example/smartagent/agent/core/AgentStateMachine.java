package com.example.smartagent.agent.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Component
@Slf4j
public class AgentStateMachine {

    public enum State {
        START,
        THINK,
        EXECUTE,
        RETHINK,
        WAIT,
        END,
        ERROR
    }

    private final Map<String, State> sessionStates = new ConcurrentHashMap<>();
    private final Map<String, State> previousStates = new ConcurrentHashMap<>();
    private final Map<String, Map<State, BiConsumer<String, State>>> stateListeners = new ConcurrentHashMap<>();

    public void init(String sessionId) {
        sessionStates.put(sessionId, State.START);
        previousStates.put(sessionId, null);
        log.info("State machine initialized for session: {}, state: START", sessionId);
        notifyListeners(sessionId, null, State.START);
    }

    public State getState(String sessionId) {
        return sessionStates.getOrDefault(sessionId, State.START);
    }

    public State getPreviousState(String sessionId) {
        return previousStates.get(sessionId);
    }

    public void setState(String sessionId, State newState) {
        State oldState = sessionStates.get(sessionId);
        previousStates.put(sessionId, oldState);
        sessionStates.put(sessionId, newState);
        log.debug("State machine state changed for session: {}, {} -> {}", sessionId, oldState, newState);
        notifyListeners(sessionId, oldState, newState);
    }

    public boolean transitionTo(String sessionId, State targetState) {
        State currentState = getState(sessionId);
        if (!canTransition(currentState, targetState)) {
            log.warn("Invalid state transition for session: {}, {} -> {}",
                    sessionId, currentState, targetState);
            return false;
        }
        setState(sessionId, targetState);
        return true;
    }

    private boolean canTransition(State from, State to) {
        if (from == null || to == null) {
            return false;
        }
        return switch (from) {
            case START -> to == State.THINK;
            case THINK -> to == State.EXECUTE || to == State.END;
            case EXECUTE -> to == State.RETHINK || to == State.END;
            case RETHINK -> to == State.THINK || to == State.END;
            case WAIT -> to == State.THINK || to == State.END;
            case ERROR -> to == State.END;
            case END -> false;
        };
    }

    public void end(String sessionId) {
        setState(sessionId, State.END);
        log.info("State machine ended for session: {}", sessionId);
    }

    public void error(String sessionId) {
        setState(sessionId, State.ERROR);
        log.error("State machine error state for session: {}", sessionId);
    }

    public void clear(String sessionId) {
        sessionStates.remove(sessionId);
        previousStates.remove(sessionId);
        stateListeners.remove(sessionId);
        log.debug("State machine cleared for session: {}", sessionId);
    }

    public boolean isEnded(String sessionId) {
        return getState(sessionId) == State.END;
    }

    public boolean isError(String sessionId) {
        return getState(sessionId) == State.ERROR;
    }

    public String getTrace(String sessionId) {
        State current = getState(sessionId);
        State previous = getPreviousState(sessionId);
        return String.format("Previous: %s -> Current: %s",
                previous != null ? previous : "null", current);
    }

    public void addStateListener(String sessionId, State state, BiConsumer<String, State> listener) {
        stateListeners.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                     .put(state, listener);
    }

    public void removeStateListener(String sessionId, State state) {
        Map<State, BiConsumer<String, State>> listeners = stateListeners.get(sessionId);
        if (listeners != null) {
            listeners.remove(state);
        }
    }

    private void notifyListeners(String sessionId, State oldState, State newState) {
        Map<State, BiConsumer<String, State>> listeners = stateListeners.get(sessionId);
        if (listeners != null) {
            BiConsumer<String, State> listener = listeners.get(newState);
            if (listener != null) {
                try {
                    listener.accept(sessionId, newState);
                } catch (Exception e) {
                    log.error("Error notifying state listener for session: {}, state: {}",
                            sessionId, newState, e);
                }
            }
        }
    }
}
