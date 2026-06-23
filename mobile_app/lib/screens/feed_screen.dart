import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../services/api_service.dart';
import '../theme/app_theme.dart';
import '../widgets/product_bottom_sheet.dart';

class FeedScreen extends StatefulWidget {
  const FeedScreen({super.key});

  @override
  State<FeedScreen> createState() => _FeedScreenState();
}

class _FeedScreenState extends State<FeedScreen> {
  List<Map<String, dynamic>> videos = [];
  bool isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadFeed();
  }

  Future<void> _loadFeed() async {
    final data = await ApiService.fetchFeed();
    setState(() {
      videos = data;
      isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return const Scaffold(
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: AppTheme.warmCream,
                ),
              ),
              SizedBox(height: 16),
              Text(
                'Loading your feed...',
                style: TextStyle(color: AppTheme.muted, fontSize: 13),
              ),
            ],
          ),
        ),
      );
    }

    if (videos.isEmpty) {
      return Scaffold(
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.auto_awesome_outlined,
                  color: AppTheme.warmGold.withValues(alpha: 0.5), size: 48),
              const SizedBox(height: 16),
              Text(
                'Your feed is brewing',
                style: GoogleFonts.dmSans(
                  color: AppTheme.warmCream,
                  fontSize: 20,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'AI is generating fresh looks for you',
                style: TextStyle(color: AppTheme.muted, fontSize: 14),
              ),
            ],
          ),
        ),
      );
    }

    return Scaffold(
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        title: Text(
          'MESHADA',
          style: GoogleFonts.dmSans(
            fontSize: 16,
            fontWeight: FontWeight.w700,
            letterSpacing: 3,
            color: AppTheme.warmCream,
          ),
        ),
        centerTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded, color: AppTheme.warmCream),
            onPressed: _loadFeed,
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: PageView.builder(
        scrollDirection: Axis.vertical,
        itemCount: videos.length,
        itemBuilder: (context, index) {
          final video = videos[index];
          return _VideoCard(
            videoUrl: video['url'] ?? '',
            modelName: video['modelName'] ?? 'Model',
            itemName: video['itemName'] ?? 'Item',
            affiliateLink: video['affiliateLink'] ?? '',
            videoData: video,
          );
        },
      ),
    );
  }
}

class _VideoCard extends StatefulWidget {
  final String videoUrl;
  final String modelName;
  final String itemName;
  final String affiliateLink;
  final Map<String, dynamic> videoData;

  const _VideoCard({
    required this.videoUrl,
    required this.modelName,
    required this.itemName,
    required this.affiliateLink,
    required this.videoData,
  });

  @override
  State<_VideoCard> createState() => _VideoCardState();
}

class _VideoCardState extends State<_VideoCard>
    with SingleTickerProviderStateMixin {
  late VideoPlayerController _controller;
  bool _hasError = false;
  bool _isLiked = false;
  bool _isSaved = false;
  bool _isGenerating = false;
  late AnimationController _heartAnimation;

  void _triggerGeneration() async {
    if (_isGenerating) return;
    setState(() => _isGenerating = true);
    
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Starting AI Video Generation in background...'),
        duration: Duration(seconds: 3),
      ),
    );
    
    final success = await ApiService.triggerVideoGeneration(
      productName: widget.itemName,
      productImageUrl: widget.videoUrl,
      productDescription: 'Fashion item styled by ${widget.modelName}',
      productType: 'fashion',
      affiliateLink: widget.affiliateLink,
    );
    
    if (mounted) {
      setState(() => _isGenerating = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(success 
            ? 'Generation triggered! Refresh feed in a few moments.' 
            : 'Failed to start video generation.'),
          duration: const Duration(seconds: 4),
        ),
      );
    }
  }

  @override
  void initState() {
    super.initState();
    _heartAnimation = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 300),
    );
    _controller = VideoPlayerController.networkUrl(Uri.parse(widget.videoUrl))
      ..initialize().then((_) {
        if (mounted) {
          setState(() {});
          _controller.play();
          _controller.setLooping(true);
        }
      }).catchError((error) {
        if (mounted) setState(() => _hasError = true);
      });
  }

  @override
  void dispose() {
    _controller.dispose();
    _heartAnimation.dispose();
    super.dispose();
  }

  Future<void> _launchLink() async {
    final Uri url = Uri.parse(widget.affiliateLink);
    if (!await launchUrl(url, mode: LaunchMode.externalApplication)) {
      // silently fail
    }
  }

  void _showProductSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (_) => ProductBottomSheet(videoData: widget.videoData),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        // Video / Fallback background
        GestureDetector(
          onTap: () {
            if (!_hasError && _controller.value.isInitialized) {
              _controller.value.isPlaying
                  ? _controller.pause()
                  : _controller.play();
              setState(() {});
            }
          },
          onDoubleTap: () {
            setState(() => _isLiked = !_isLiked);
            _heartAnimation.forward().then((_) => _heartAnimation.reverse());
          },
          child: Container(
            color: AppTheme.scaffoldBg,
            child: _hasError
                ? _buildFallbackView()
                : _controller.value.isInitialized
                    ? SizedBox.expand(
                        child: FittedBox(
                          fit: BoxFit.cover,
                          child: SizedBox(
                            width: _controller.value.size.width,
                            height: _controller.value.size.height,
                            child: VideoPlayer(_controller),
                          ),
                        ),
                      )
                    : const Center(
                        child: SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: AppTheme.warmCream,
                          ),
                        ),
                      ),
          ),
        ),

        // Pause indicator
        if (!_hasError &&
            _controller.value.isInitialized &&
            !_controller.value.isPlaying)
          Center(
            child: Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.black.withValues(alpha: 0.4),
                shape: BoxShape.circle,
              ),
              child:
                  const Icon(Icons.play_arrow_rounded, color: Colors.white, size: 40),
            ),
          ),

        // Right side action buttons
        Positioned(
          right: 16,
          bottom: 180,
          child: Column(
            children: [
              _ActionButton(
                icon: _isLiked
                    ? Icons.favorite_rounded
                    : Icons.favorite_border_rounded,
                label: '2.4k',
                color: _isLiked ? AppTheme.softRose : Colors.white,
                onTap: () => setState(() => _isLiked = !_isLiked),
              ),
              const SizedBox(height: 20),
              _ActionButton(
                icon: Icons.chat_bubble_outline_rounded,
                label: '148',
                onTap: () {},
              ),
              const SizedBox(height: 20),
              _ActionButton(
                icon: _isSaved
                    ? Icons.bookmark_rounded
                    : Icons.bookmark_border_rounded,
                label: 'Save',
                color: _isSaved ? AppTheme.warmGold : Colors.white,
                onTap: () => setState(() => _isSaved = !_isSaved),
              ),
              const SizedBox(height: 20),
              _ActionButton(
                icon: Icons.share_outlined,
                label: 'Share',
                onTap: () {},
              ),
            ],
          ),
        ),

        // Bottom info + CTA
        Positioned(
          bottom: 0,
          left: 0,
          right: 72,
          child: Container(
            padding: const EdgeInsets.fromLTRB(20, 40, 20, 24),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [
                  Colors.transparent,
                  Colors.black.withValues(alpha: 0.7),
                  Colors.black.withValues(alpha: 0.85),
                ],
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                // Model name
                Row(
                  children: [
                    Container(
                      width: 32,
                      height: 32,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(
                            color: AppTheme.warmGold, width: 1.5),
                        color: AppTheme.cardBg,
                      ),
                      child: const Icon(Icons.person_rounded,
                          size: 18, color: AppTheme.warmGold),
                    ),
                    const SizedBox(width: 10),
                    Flexible(
                      child: Text(
                        '@${widget.modelName}',
                        style: GoogleFonts.dmSans(
                          color: Colors.white,
                          fontWeight: FontWeight.w600,
                          fontSize: 15,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        border: Border.all(
                            color: AppTheme.warmGold.withValues(alpha: 0.5)),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(
                        'AI MODEL',
                        style: GoogleFonts.dmSans(
                          color: AppTheme.warmGold,
                          fontSize: 9,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 1.2,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),

                // Caption
                Text(
                  'Obsessed with this ${widget.itemName} ✨',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 14,
                    height: 1.3,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 16),

                // Shop CTA
                GestureDetector(
                  onTap: _launchLink,
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                        vertical: 14, horizontal: 24),
                    decoration: BoxDecoration(
                      color: AppTheme.warmCream,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.shopping_bag_outlined,
                            size: 18, color: AppTheme.scaffoldBg),
                        const SizedBox(width: 8),
                        Text(
                          'Shop This Look',
                          style: GoogleFonts.dmSans(
                            color: AppTheme.scaffoldBg,
                            fontWeight: FontWeight.w700,
                            fontSize: 14,
                            letterSpacing: 0.3,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildFallbackView() {
    final isImage = widget.videoUrl.contains('.jpg') ||
        widget.videoUrl.contains('.jpeg') ||
        widget.videoUrl.contains('.png') ||
        widget.videoUrl.contains('.webp') ||
        widget.videoUrl.contains('unsplash.com') ||
        widget.videoUrl.contains('promodirect.com');

    return Stack(
      fit: StackFit.expand,
      children: [
        if (isImage)
          CachedNetworkImage(
            imageUrl: widget.videoUrl,
            fit: BoxFit.cover,
            placeholder: (_, __) => Container(color: AppTheme.scaffoldBg),
            errorWidget: (_, __, ___) => Container(color: AppTheme.scaffoldBg),
          )
        else
          Container(color: AppTheme.scaffoldBg),
        
        // Dim overlay to make text readable
        Container(
          color: Colors.black.withValues(alpha: 0.5),
        ),

        Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(
                      color: AppTheme.warmGold.withValues(alpha: 0.3), width: 1),
                ),
                child: Icon(
                  isImage ? Icons.auto_awesome_outlined : Icons.play_arrow_rounded,
                  color: AppTheme.warmGold.withValues(alpha: 0.6),
                  size: 48,
                ),
              ),
              const SizedBox(height: 20),
              Text(
                widget.itemName,
                textAlign: TextAlign.center,
                style: GoogleFonts.dmSans(
                  color: AppTheme.warmCream,
                  fontSize: 20,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                'by ${widget.modelName}',
                style: const TextStyle(color: AppTheme.muted, fontSize: 14),
              ),
              const SizedBox(height: 28),
              if (isImage)
                _isGenerating
                    ? const SizedBox(
                        width: 24,
                        height: 24,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: AppTheme.warmGold,
                        ),
                      )
                    : ElevatedButton.icon(
                        onPressed: _triggerGeneration,
                        icon: const Icon(Icons.bolt, size: 16),
                        label: const Text('Generate AI Video Walk'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.warmGold,
                          foregroundColor: AppTheme.scaffoldBg,
                          textStyle: GoogleFonts.dmSans(
                            fontWeight: FontWeight.bold,
                            fontSize: 13,
                          ),
                          padding: const EdgeInsets.symmetric(
                              horizontal: 20, vertical: 12),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(30),
                          ),
                        ),
                      ),
            ],
          ),
        ),
      ],
    );
  }
}

class _ActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final Color color;

  const _ActionButton({
    required this.icon,
    required this.label,
    required this.onTap,
    this.color = Colors.white,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Column(
        children: [
          Icon(icon, color: color, size: 28),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              color: color.withValues(alpha: 0.8),
              fontSize: 11,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }
}
