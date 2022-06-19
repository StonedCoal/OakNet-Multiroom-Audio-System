#pragma once

#include <stdlib.h>
#include <cstdint>


// This is a FIFO ringbuffer which can handle bunches of data
class Buffer
{
private:
    uint8_t* data = nullptr;
    unsigned int size;
    unsigned int beginOffset = 0;
    unsigned int endOffset = 0;
    unsigned int capacity = 0;

public:
    Buffer(unsigned int);
    ~Buffer();

    bool putBunch(uint8_t*, int);
    bool popBunch(uint8_t*, int);

    int getCapacity();
};


