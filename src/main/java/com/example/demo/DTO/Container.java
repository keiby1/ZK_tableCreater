package com.example.demo.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Container {
    String name;
    int CpuRq;
    int CpuLim;
    int MemRq;
    int MemLim;
    int CpuMaxPercent;
    int CpuAvgPercent;
    int CpuMaxAbs;
    int MemMaxPercent;
    int MemAvgPercent;
    int MemMaxAbs;

}
