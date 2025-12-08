package com.example.demo.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedList;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Deployment {
    String name;
    int podCount;

    LinkedList<Container> containers = new LinkedList<>();
}
