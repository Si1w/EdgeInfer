# EdgeInfer

This is a repo for Android application of Powerinfer.

## Setup and Installation

### Prerequisites

- Android Studio 2024.2.1 or later

### Installation

1. Clone the repo to your local machine using the following command:

```bash
git clone https://github.com/SJTU-IPADS/PowerInfer.git
cd android
```

2. Open the project in Android Studio.

3. In Android Studio, click on `setting`->`SDK Manager`->`SDK Tools` and install `NDK` and `CMake`.

4. Set environment variables

```bash
# For example
export ANDROID_NDK=$HOME/Library/Android/sdk/ndk/{ndk-version}
export PATH="/Path/To/Android/sdk/platform-tools:$PATH"
```

5. Download the model from Huggingface like

- [PowerInfer/ReluLLaMA-7B-PowerInfer-GGUF](https://huggingface.co/PowerInfer/ReluLLaMA-7B-PowerInfer-GGUF)

6. Quantize the model(**Recommended!**)

```bash
cd PowerInfer
./build/bin/quantize /PATH/TO/MODEL /PATH/TO/OUTPUT/QUANTIZED/MODEL Q4_0
```

7. Use `adb` or something else to transfer the model to phone.

```bash
adb shell "mkdir /data/local/tmp/powerinfer"
adb push /Path/To/model.gguf /data/local/tmp/powerinfer/
```

8. Build and run the project in Android Studio.

## Paper and Citation
More technical details can be found in [paper](https://ipads.se.sjtu.edu.cn/_media/publications/powerinfer-20231219.pdf).

```bibtex
@misc{song2023powerinfer,
      title={PowerInfer: Fast Large Language Model Serving with a Consumer-grade GPU},
      author={Yixin Song and Zeyu Mi and Haotong Xie and Haibo Chen},
      year={2023},
      eprint={2312.12456},
      archivePrefix={arXiv},
      primaryClass={cs.LG}
}
```
