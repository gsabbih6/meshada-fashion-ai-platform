import { Composition } from "remotion";
import { UGCVideo } from "./UGCVideo";

// You can pass the JSON output from the Python orchestrator into here via Remotion Props.
export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Composition
        id="ugc-tiktok-format"
        component={UGCVideo}
        durationInFrames={300} // 10 seconds at 30fps
        fps={30}
        width={1080}
        height={1920} // Vertical aspect ratio for TikTok/Reels/Shorts
        defaultProps={{
          videoUrl: "https://meshada.mock/vton/model_1_wearing_item_lipsynced.mp4",
          audioUrl: "https://meshada.mock/output_assets/prod_12345_model_1_voiceover.mp3",
          captionText: "Obsessed with this Silk Slip Dress!",
          productImageUrl: "https://example.com/products/summer_dress.jpg",
          productName: "Silk Slip Dress",
        }}
      />
    </>
  );
};
