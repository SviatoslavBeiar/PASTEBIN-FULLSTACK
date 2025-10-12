package com.example.demo;

import org.springframework.stereotype.Component;
@Component
public class Controller {

   // Service service;

//    public Controller(Service service) {
//        this.service = service;
//    }
Service service = new Service();
    public void print(int str){

        int i = service.inc(str);
        System.out.println(i);
    }
}
