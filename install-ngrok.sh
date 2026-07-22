#!bin/bash

mkdir -p ~/.local/bin
cd /tmp
curl -sSL https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz -o ngrok.tgz
tar -xvzf ngrok.tgz -C ~/.local/bin
rm ngrok.tgz

# Add ~/.local/bin to PATH in .zshrc
if [[ ":$PATH:" != *":$HOME/.local/bin:"* ]]; then
  echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.zshrc
  export PATH="$HOME/.local/bin:$PATH"
fi

echo "--- Installation complete! ---"
ngrok version


# ngrok config add-authtoken <YOUR_AUTHTOKEN_HERE>