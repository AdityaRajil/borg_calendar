package net.sf.borg.common;

import javax.swing.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ModalMessageServer {

    static volatile private ModalMessageServer singleton = null;

    static public ModalMessageServer getReference() {
        if (singleton == null) {
            ModalMessageServer b = new ModalMessageServer();
            singleton = b;
        }
        return (singleton);
    }

    private ModalMessage modalMessage = null;

    private BlockingQueue<String> messageQ = new LinkedBlockingQueue<>();

    private ModalMessageServer() {
        Thread t = new Thread(() -> processMessages());
        t.start();
    }
    private void processMessages() {
        try {
            while (true) {
                String msg = messageQ.take();
                if (msg.startsWith("lock:")) {
                    showLockMessage(msg.substring(5));
                } else if (msg.startsWith("log:")) {
                    processLogMessage(msg.substring(4));
                } else if (msg.equals("unlock")) {
                    processUnlockMessage();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void showLockMessage(String lockmsg) {
        SwingUtilities.invokeLater(() -> {
            if (modalMessage == null || !modalMessage.isShowing()) {
                modalMessage = new ModalMessage(lockmsg, false);
                modalMessage.setVisible(true);
            } else {
                modalMessage.appendText(lockmsg);
            }
            modalMessage.setEnabled(false);
            modalMessage.toFront();
        });
    }

//    private void processLockMessage(String lockmsg) {
//        SwingUtilities.invokeLater(() -> {
//            if (modalMessage == null || !modalMessage.isShowing()) {
//                modalMessage = new ModalMessage(lockmsg, false);
//                modalMessage.setVisible(true);
//            } else {
//                modalMessage.appendText(lockmsg);
//            }
//            modalMessage.setEnabled(false);
//            modalMessage.toFront();
//        });
//    }

    private void processLogMessage(String lockmsg) {
        SwingUtilities.invokeLater(() -> {
            if (modalMessage != null && modalMessage.isShowing()) {
                modalMessage.appendText(lockmsg);
            }
        });
    }

    private void processUnlockMessage() {
        SwingUtilities.invokeLater(() -> {
            if (modalMessage != null && modalMessage.isShowing()) {
                modalMessage.setEnabled(true);
            }
        });
    }

    public void sendMessage(String msg){
        messageQ.add(msg);
    }

    public void sendLogMessage(String msg) {
        sendMessage("log:" + msg);
    }

}
